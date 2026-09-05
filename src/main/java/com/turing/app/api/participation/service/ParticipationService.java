package com.turing.app.api.participation.service;

import com.turing.app.api.audit.service.AuditService;
import com.turing.app.api.notification.service.ParticipationMailService;
import com.turing.app.api.participation.dto.*;
import com.turing.app.api.participation.exception.ParticipationException;
import com.turing.app.api.participation.repository.ParticipationRepository;
import com.turing.app.api.participation.repository.ParticipationRepository.Activity;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipationService {
  private final ParticipationRepository repository;
  private final ParticipationMapper mapper;
  private final ParticipationMailService mail;
  private final AuditService audit;
  private final Clock clock;
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd.MM.yyyy EEEE", Locale.forLanguageTag("tr"));
  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr"))
          .withZone(ParticipationMapper.ZONE);

  public ParticipationService(
      ParticipationRepository repository,
      ParticipationMapper mapper,
      ParticipationMailService mail,
      AuditService audit,
      Clock clock) {
    this.repository = repository;
    this.mapper = mapper;
    this.mail = mail;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResponse<MealWeekSummary> weeks(int page, int size) {
    validatePage(page, size);
    return page(repository.weeks(page, size), page, size, repository.weekCount(), "weekStart,desc");
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public MealWeekResponse week(UUID id, UUID user) {
    var week = requireWeek(id);
    return new MealWeekResponse(
        id,
        week.weekStart(),
        repository.days(id, user).stream().map(mapper::response).toList(),
        repository.version(user, id.toString()),
        repository.weekVersion(id));
  }

  @Transactional
  public MealWeekResponse createWeek(UUID actor, MealWeekRequest request, String ip) {
    if (request.weekStart().getDayOfWeek() != DayOfWeek.MONDAY) {
      throw bad("MEAL_WEEK_START", "Hafta başlangıcı pazartesi olmalıdır.");
    }
    Set<LocalDate> dates = new HashSet<>();
    LocalDate today = LocalDate.now(clock.withZone(ParticipationMapper.ZONE));
    for (MealDayRequest day : request.days()) {
      if (day.date().isBefore(request.weekStart())
          || day.date().isAfter(request.weekStart().plusDays(6))
          || !dates.add(day.date())) {
        throw bad(
            "MEAL_DAY_INVALID", "Yemek günleri seçilen haftada ve birbirinden farklı olmalıdır.");
      }
      if (!day.date().isAfter(today)) {
        throw bad("MEAL_DAY_PAST", "Yemek kayıtları yalnız gelecek günler için açılabilir.");
      }
    }
    UUID id = UUID.randomUUID();
    repository.createWeek(id, request.weekStart(), actor, clock.instant());
    for (MealDayRequest day : request.days()) {
      repository.createActivity(
          UUID.randomUUID(), id, "Yemek", "", day.date(), null, "", actor, clock.instant());
    }
    audit.record(actor, "MEAL_WEEK_CREATED", "MEAL_WEEK", id, "{}", "{}", ip);
    return week(id, actor);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public EventsResponse events(UUID user, int page, int size) {
    validatePage(page, size);
    var values = repository.events(user, page, size).stream().map(mapper::response).toList();
    return new EventsResponse(
        page(values, page, size, repository.eventCount(), "startsAt,desc"),
        repository.version(user, "events"));
  }

  @Transactional
  public ActivityResponse createEvent(UUID actor, EventRequest request, String ip) {
    if (!request.startsAt().isAfter(clock.instant())) {
      throw bad("EVENT_START_PAST", "Etkinlik başlangıcı gelecekte olmalıdır.");
    }
    UUID id = UUID.randomUUID();
    repository.createActivity(
        id,
        null,
        request.title().trim(),
        request.description().trim(),
        null,
        request.startsAt(),
        request.location().trim(),
        actor,
        clock.instant());
    audit.record(actor, "EVENT_CREATED", "EVENT", id, "{}", "{}", ip);
    return mapper.response(requireActivity(id, actor));
  }

  @Transactional
  public MealWeekResponse updateWeek(
      UUID actor, UUID id, MealWeekUpdateRequest request, String ip) {
    repository.lockWeek(id);
    var week = requireWeek(id);
    checkAdminVersion(repository.weekVersion(id), request.version());
    Set<LocalDate> desired = new HashSet<>();
    for (MealDayRequest day : request.days()) {
      if (day.date().isBefore(week.weekStart())
          || day.date().isAfter(week.weekStart().plusDays(6))
          || !desired.add(day.date())) {
        throw bad("MEAL_DAY_INVALID", "Günler aynı hafta içinde ve birbirinden farklı olmalıdır.");
      }
    }
    List<Activity> existing = repository.days(id, actor);
    Set<LocalDate> previous = new HashSet<>();
    for (Activity day : existing) {
      previous.add(day.date());
      if (!desired.contains(day.date())) {
        requireOpen(day);
        if (repository.participantCount(day.id()) > 0) {
          throw new ParticipationException(
              HttpStatus.CONFLICT,
              "MEAL_DAY_HAS_PARTICIPANTS",
              "Katılımcısı olan yemek günü kaldırılamaz. Öğrenci kayıtları korunmalıdır.");
        }
      }
    }
    LocalDate today = LocalDate.now(clock.withZone(ParticipationMapper.ZONE));
    for (LocalDate date : desired) {
      if (!previous.contains(date) && !date.isAfter(today))
        throw bad("MEAL_DAY_PAST", "Yalnız gelecek günler eklenebilir.");
    }
    if (previous.equals(desired)) return week(id, actor);
    for (Activity day : existing) if (!desired.contains(day.date())) repository.removeDay(day.id());
    for (LocalDate date : desired)
      if (!previous.contains(date))
        repository.createActivity(
            UUID.randomUUID(), id, "Yemek", "", date, null, "", actor, clock.instant());
    repository.incrementWeekVersion(id);
    audit.record(actor, "MEAL_WEEK_UPDATED", "MEAL_WEEK", id, "{}", "{}", ip);
    return week(id, actor);
  }

  @Transactional
  public ActivityResponse updateEvent(UUID actor, UUID id, EventUpdateRequest request, String ip) {
    repository.lockActivity(id);
    Activity existing = requireActivity(id, actor);
    if (existing.weekId() != null)
      throw bad("EVENT_INVALID", "Yemek günü etkinlik olarak düzenlenemez.");
    checkAdminVersion(existing.version(), request.version());
    requireOpen(existing);
    if (!request.startsAt().isAfter(clock.instant()))
      throw bad("EVENT_START_PAST", "Etkinlik başlangıcı gelecekte olmalıdır.");
    if (existing.title().equals(request.title().trim())
        && existing.description().equals(request.description().trim())
        && existing.startsAt().equals(request.startsAt())
        && existing.location().equals(request.location().trim())) return mapper.response(existing);
    repository.updateEvent(id, request);
    audit.record(actor, "EVENT_UPDATED", "EVENT", id, "{}", "{}", ip);
    String body =
        "Katıldığınız etkinliğin bilgileri güncellendi.\n\n"
            + request.title().trim()
            + "\n"
            + TIME.format(request.startsAt())
            + " (Türkiye saati)\n"
            + request.location().trim()
            + "\n\n"
            + request.description().trim();
    for (UUID user : repository.participantIds(id))
      mail.enqueue(user, "Etkinlik bilgileri güncellendi", body);
    return mapper.response(requireActivity(id, actor));
  }

  private void checkAdminVersion(long current, long submitted) {
    if (current != submitted)
      throw new ParticipationException(
          HttpStatus.CONFLICT,
          "ACTIVITY_VERSION_CONFLICT",
          "Kayıt başka bir işlemde güncellendi. Formu kapatıp güncel kaydı yeniden açın.");
  }

  @Transactional
  public SelectionResult saveMeals(UUID user, UUID weekId, MealSelectionRequest request) {
    repository.lockWeek(weekId);
    var week = requireWeek(weekId);
    String scope = weekId.toString();
    long version = repository.lockSelection(user, scope);
    List<Activity> days = repository.days(weekId, user);
    Set<UUID> ids = new HashSet<>();
    days.forEach(day -> ids.add(day.id()));
    if (!ids.containsAll(request.dayIds())) {
      throw bad("MEAL_DAY_INVALID", "Seçilen gün bu yemek haftasına ait değil.");
    }
    List<Activity> changes =
        days.stream()
            .filter(day -> day.attending() != request.dayIds().contains(day.id()))
            .toList();
    if (changes.isEmpty()) return new SelectionResult(version, false);
    checkVersion(version, request.version());
    changes.forEach(this::requireOpen);
    for (Activity day : changes)
      repository.setAttendance(
          user, day.id(), request.dayIds().contains(day.id()), clock.instant());
    repository.incrementVersion(user, scope);
    String selected =
        days.stream()
            .filter(day -> request.dayIds().contains(day.id()))
            .map(day -> "• " + DATE.format(day.date()))
            .reduce((a, b) -> a + "\n" + b)
            .orElse("Bu hafta için yemek kaydınız bulunmuyor.");
    mail.enqueue(
        user,
        "Haftalık yemek kaydınız güncellendi",
        DATE.format(week.weekStart()) + " haftası yemek seçimleriniz:\n\n" + selected);
    return new SelectionResult(version + 1, true);
  }

  @Transactional
  public SelectionResult saveEvents(UUID user, EventSelectionRequest request) {
    long version = repository.lockSelection(user, "events");
    request.changes().stream()
        .map(EventSelection::eventId)
        .distinct()
        .sorted()
        .forEach(repository::lockActivity);
    Set<UUID> ids = new HashSet<>();
    List<Activity> changed = new ArrayList<>();
    Map<UUID, Boolean> desired = new HashMap<>();
    for (EventSelection selection : request.changes()) {
      if (!ids.add(selection.eventId()))
        throw bad("EVENT_DUPLICATE", "Aynı etkinlik birden fazla kez gönderilemez.");
      Activity activity = requireActivity(selection.eventId(), user);
      if (activity.weekId() != null)
        throw bad("EVENT_INVALID", "Yemek günü etkinlik olarak kaydedilemez.");
      desired.put(activity.id(), selection.attending());
      if (activity.attending() != selection.attending()) changed.add(activity);
    }
    if (changed.isEmpty()) return new SelectionResult(version, false);
    checkVersion(version, request.version());
    changed.forEach(this::requireOpen);
    for (Activity activity : changed)
      repository.setAttendance(user, activity.id(), desired.get(activity.id()), clock.instant());
    repository.incrementVersion(user, "events");
    StringBuilder body = new StringBuilder("Etkinlik katılım değişiklikleriniz:\n\n");
    for (Activity activity : changed)
      body.append(desired.get(activity.id()) ? "Katılacağım: " : "Katılım iptal edildi: ")
          .append(activity.title())
          .append(" — ")
          .append(TIME.format(activity.startsAt()))
          .append("\n");
    body.append("\nKatılacağınız gelecek etkinlikler (Türkiye saati):\n");
    List<Activity> selected = repository.registeredEvents(user, clock.instant());
    if (selected.isEmpty()) body.append("Etkinlik kaydınız bulunmuyor.\n");
    for (Activity activity : selected)
      body.append("• ")
          .append(activity.title())
          .append(" — ")
          .append(TIME.format(activity.startsAt()))
          .append(activity.location().isBlank() ? "" : " — " + activity.location())
          .append("\n");
    mail.enqueue(user, "Etkinlik kayıtlarınız güncellendi", body.toString());
    return new SelectionResult(version + 1, true);
  }

  @Transactional(readOnly = true)
  public PageResponse<ParticipantResponse> participants(UUID id, int page, int size) {
    validatePage(page, size);
    requireActivity(id, null);
    return page(
        repository.participants(id, page, size),
        page,
        size,
        repository.participantCount(id),
        "firstName,asc");
  }

  private MealWeekSummary requireWeek(UUID id) {
    return repository.week(id).orElseThrow(() -> missing("Yemek haftası bulunamadı."));
  }

  private Activity requireActivity(UUID id, UUID user) {
    return repository
        .activity(id, user)
        .orElseThrow(() -> missing("Yemek günü veya etkinlik bulunamadı."));
  }

  private void requireOpen(Activity activity) {
    if (!mapper.registrationOpen(activity))
      throw new ParticipationException(
          HttpStatus.CONFLICT, "REGISTRATION_CLOSED", "Kayıt süresi sona erdi. Sayfayı yenileyin.");
  }

  private void checkVersion(long current, long submitted) {
    if (current != submitted)
      throw new ParticipationException(
          HttpStatus.CONFLICT,
          "SELECTION_VERSION_CONFLICT",
          "Seçimleriniz başka bir sekmede güncellendi. Sayfayı yenileyin.");
  }

  private void validatePage(int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      throw bad("INVALID_PAGE", "Sayfa en az 0, sayfa boyutu 1–100 arasında olmalıdır.");
  }

  private <T> PageResponse<T> page(List<T> content, int page, int size, long total, String sort) {
    return new PageResponse<>(content, page, size, total, (int) ((total + size - 1) / size), sort);
  }

  private ParticipationException bad(String code, String message) {
    return new ParticipationException(HttpStatus.BAD_REQUEST, code, message);
  }

  private ParticipationException missing(String message) {
    return new ParticipationException(HttpStatus.NOT_FOUND, "PARTICIPATION_NOT_FOUND", message);
  }
}
