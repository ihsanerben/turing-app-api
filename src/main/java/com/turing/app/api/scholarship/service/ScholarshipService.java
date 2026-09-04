package com.turing.app.api.scholarship.service;

import com.turing.app.api.audit.service.AuditService;
import com.turing.app.api.scholarship.dto.*;
import com.turing.app.api.scholarship.entity.*;
import com.turing.app.api.scholarship.exception.ScholarshipException;
import com.turing.app.api.scholarship.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ScholarshipService {
  private static final Set<PeriodStatus> PUBLIC_STATUSES =
      Set.of(PeriodStatus.SCHEDULED, PeriodStatus.OPEN);
  private final ScholarshipProgramRepository programs;
  private final ApplicationPeriodRepository periods;
  private final FormDefinitionRepository forms;
  private final AuditService audit;
  private final ObjectMapper json;
  private final Clock clock;

  public ScholarshipService(
      ScholarshipProgramRepository programs,
      ApplicationPeriodRepository periods,
      FormDefinitionRepository forms,
      AuditService audit,
      ObjectMapper json,
      Clock clock) {
    this.programs = programs;
    this.periods = periods;
    this.forms = forms;
    this.audit = audit;
    this.json = json;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<ProgramResponse> adminPrograms(boolean includeArchived) {
    return (includeArchived
            ? programs.findAllByOrderByNameAsc()
            : programs.findByActiveTrueOrderByNameAsc())
        .stream().map(ProgramResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public ProgramResponse program(UUID id) {
    return ProgramResponse.from(findProgram(id));
  }

  @Transactional
  public ProgramResponse createProgram(UUID actor, ProgramRequest request, String ip) {
    String slug = request.slug().toLowerCase(Locale.ROOT);
    if (programs.existsBySlugIgnoreCase(slug))
      throw conflict("SLUG_ALREADY_EXISTS", "Bu URL adı zaten kullanılıyor.");
    ScholarshipProgram saved =
        programs.saveAndFlush(
            ScholarshipProgram.create(
                request.name().trim(), slug, request.description().trim(), clock.instant()));
    audit.record(
        actor, "PROGRAM_CREATED", "SCHOLARSHIP_PROGRAM", saved.getId(), "{}", snapshot(saved), ip);
    return ProgramResponse.from(saved);
  }

  @Transactional
  public ProgramResponse updateProgram(UUID actor, UUID id, ProgramRequest request, String ip) {
    ScholarshipProgram value = findProgram(id);
    checkVersion(value.getVersion(), request.version());
    String slug = request.slug().toLowerCase(Locale.ROOT);
    programs
        .findBySlugIgnoreCase(slug)
        .filter(other -> !other.getId().equals(id))
        .ifPresent(
            other -> {
              throw conflict("SLUG_ALREADY_EXISTS", "Bu URL adı zaten kullanılıyor.");
            });
    String before = snapshot(value);
    value.update(request.name().trim(), slug, request.description().trim(), clock.instant());
    programs.flush();
    audit.record(actor, "PROGRAM_UPDATED", "SCHOLARSHIP_PROGRAM", id, before, snapshot(value), ip);
    return ProgramResponse.from(value);
  }

  @Transactional
  public ProgramResponse archiveProgram(UUID actor, UUID id, Long version, String ip) {
    ScholarshipProgram value = findProgram(id);
    checkVersion(value.getVersion(), version);
    ApplicationPeriod latest =
        periods.findByProgramIdOrderByStartsAtDesc(id).stream().findFirst().orElse(null);
    if (latest == null
        || (latest.getStatus() != PeriodStatus.CLOSED
            && latest.getStatus() != PeriodStatus.COMPLETED))
      throw conflict("PROGRAM_NOT_FINISHED", "Yalnız bitmiş program arşive alınabilir.");
    String before = snapshot(value);
    value.archive(clock.instant());
    programs.flush();
    audit.record(actor, "PROGRAM_ARCHIVED", "SCHOLARSHIP_PROGRAM", id, before, snapshot(value), ip);
    return ProgramResponse.from(value);
  }

  @Transactional
  public ProgramResponse restoreProgram(UUID actor, UUID id, Long version, String ip) {
    ScholarshipProgram value = findProgram(id);
    checkVersion(value.getVersion(), version);
    if (value.isActive()) throw conflict("PROGRAM_NOT_ARCHIVED", "Program zaten arşiv dışında.");
    String before = snapshot(value);
    value.restore(clock.instant());
    programs.flush();
    audit.record(actor, "PROGRAM_RESTORED", "SCHOLARSHIP_PROGRAM", id, before, snapshot(value), ip);
    return ProgramResponse.from(value);
  }

  @Transactional(readOnly = true)
  public List<PeriodResponse> adminPeriods(UUID programId) {
    return periods.findByProgramIdOrderByStartsAtDesc(programId).stream()
        .map(PeriodResponse::from)
        .toList();
  }

  @Transactional
  public PeriodResponse createPeriod(UUID actor, PeriodRequest request, String ip) {
    validateDates(request.startsAt(), request.endsAt());
    ScholarshipProgram program = findProgram(request.programId());
    if (!program.isActive()) throw bad("PROGRAM_ARCHIVED", "Arşivlenmiş programa dönem eklenemez.");
    ApplicationPeriod saved =
        periods.saveAndFlush(
            ApplicationPeriod.create(
                program,
                request.name().trim(),
                request.academicYear(),
                request.startsAt(),
                request.endsAt(),
                request.maxRecipients(),
                request.allowWithdrawal(),
                clock.instant()));
    audit.record(
        actor, "PERIOD_CREATED", "APPLICATION_PERIOD", saved.getId(), "{}", snapshot(saved), ip);
    return PeriodResponse.from(saved);
  }

  @Transactional
  public PeriodResponse updatePeriod(UUID actor, UUID id, PeriodRequest request, String ip) {
    ApplicationPeriod value = findPeriod(id);
    checkVersion(value.getVersion(), request.version());
    if (value.getStatus() != PeriodStatus.DRAFT
        && value.getStatus() != PeriodStatus.SCHEDULED
        && value.getStatus() != PeriodStatus.OPEN)
      throw conflict("PERIOD_NOT_EDITABLE", "Bu durumdaki dönem düzenlenemez.");
    if (!value.getProgram().getId().equals(request.programId()))
      throw bad("PROGRAM_CANNOT_CHANGE", "Dönemin programı değiştirilemez.");
    validateDates(request.startsAt(), request.endsAt());
    if (value.getStatus() == PeriodStatus.SCHEDULED && !request.startsAt().isAfter(clock.instant()))
      throw bad("INVALID_SCHEDULE", "Planlı dönemin başlangıcı gelecekte olmalıdır.");
    String before = snapshot(value);
    value.update(
        request.name().trim(),
        request.academicYear(),
        request.startsAt(),
        request.endsAt(),
        request.maxRecipients(),
        request.allowWithdrawal(),
        clock.instant());
    periods.flush();
    audit.record(actor, "PERIOD_UPDATED", "APPLICATION_PERIOD", id, before, snapshot(value), ip);
    return PeriodResponse.from(value);
  }

  @Transactional
  public PeriodResponse transition(UUID actor, UUID id, PeriodStatusRequest request, String ip) {
    ApplicationPeriod value = findPeriod(id);
    checkVersion(value.getVersion(), request.version());
    validateTransition(value, request.status(), clock.instant());
    String before = snapshot(value);
    value.transition(request.status(), clock.instant());
    periods.flush();
    audit.record(
        actor, "PERIOD_STATUS_CHANGED", "APPLICATION_PERIOD", id, before, snapshot(value), ip);
    return PeriodResponse.from(value);
  }

  @Transactional(readOnly = true)
  public List<PublicScholarshipResponse> publicPrograms() {
    return programs.findByActiveTrueOrderByNameAsc().stream()
        .map(p -> publicResponse(p))
        .filter(v -> !v.periods().isEmpty())
        .toList();
  }

  @Transactional(readOnly = true)
  public PublicScholarshipResponse publicProgram(String slug) {
    ScholarshipProgram value =
        programs
            .findBySlugAndActiveTrue(slug)
            .orElseThrow(() -> notFound("PROGRAM_NOT_FOUND", "Başvuru programı bulunamadı."));
    PublicScholarshipResponse response = publicResponse(value);
    if (response.periods().isEmpty())
      throw notFound("PROGRAM_NOT_FOUND", "Başvuru programı bulunamadı.");
    return response;
  }

  private PublicScholarshipResponse publicResponse(ScholarshipProgram p) {
    return new PublicScholarshipResponse(
        ProgramResponse.from(p),
        periods.findByProgramIdAndStatusInOrderByStartsAtDesc(p.getId(), PUBLIC_STATUSES).stream()
            .map(PeriodResponse::from)
            .toList());
  }

  private void validateTransition(ApplicationPeriod p, PeriodStatus next, Instant now) {
    PeriodStatus current = p.getStatus();
    boolean allowed =
        switch (current) {
          case DRAFT ->
              next == PeriodStatus.SCHEDULED
                  || next == PeriodStatus.OPEN
                  || next == PeriodStatus.ARCHIVED;
          case SCHEDULED ->
              next == PeriodStatus.OPEN
                  || next == PeriodStatus.DRAFT
                  || next == PeriodStatus.CLOSED
                  || next == PeriodStatus.ARCHIVED;
          case OPEN -> next == PeriodStatus.DRAFT || next == PeriodStatus.CLOSED;
          case CLOSED -> next == PeriodStatus.COMPLETED;
          case COMPLETED -> next == PeriodStatus.ARCHIVED;
          case ARCHIVED -> false;
        };
    if (!allowed)
      throw conflict(
          "INVALID_PERIOD_TRANSITION", current + " durumundan " + next + " durumuna geçilemez.");
    if (next == PeriodStatus.SCHEDULED && !p.getStartsAt().isAfter(now))
      throw bad("INVALID_SCHEDULE", "Planlanan başlangıç gelecekte olmalıdır.");
    if (next == PeriodStatus.OPEN
        && (now.isBefore(p.getStartsAt()) || !now.isBefore(p.getEndsAt())))
      throw bad("OUTSIDE_APPLICATION_WINDOW", "Başvuru dönemi yalnız tarih aralığında açılabilir.");
    if (next == PeriodStatus.OPEN
        && !forms.existsByPeriodIdAndStatus(p.getId(), FormStatus.PUBLISHED))
      throw conflict(
          "PUBLISHED_FORM_REQUIRED", "Başvuru dönemi açılmadan önce bir form yayınlanmalıdır.");
  }

  private void validateDates(Instant start, Instant end) {
    if (!end.isAfter(start))
      throw bad("INVALID_PERIOD_DATES", "Bitiş başlangıçtan sonra olmalıdır.");
  }

  private ScholarshipProgram findProgram(UUID id) {
    return programs
        .findById(id)
        .orElseThrow(() -> notFound("PROGRAM_NOT_FOUND", "Başvuru programı bulunamadı."));
  }

  private ApplicationPeriod findPeriod(UUID id) {
    return periods
        .findById(id)
        .orElseThrow(() -> notFound("PERIOD_NOT_FOUND", "Başvuru dönemi bulunamadı."));
  }

  private void checkVersion(long current, Long requested) {
    if (requested == null || current != requested)
      throw conflict(
          "VERSION_CONFLICT", "Kayıt başka bir işlem tarafından güncellendi. Sayfayı yenileyin.");
  }

  private String snapshot(ScholarshipProgram p) {
    return json.writeValueAsString(
        Map.of(
            "name",
            p.getName(),
            "slug",
            p.getSlug(),
            "active",
            p.isActive(),
            "version",
            p.getVersion()));
  }

  private String snapshot(ApplicationPeriod p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("programId", p.getProgram().getId());
    m.put("name", p.getName());
    m.put("academicYear", p.getAcademicYear());
    m.put("startsAt", p.getStartsAt());
    m.put("endsAt", p.getEndsAt());
    m.put("status", p.getStatus());
    m.put("maxRecipients", p.getMaxRecipients());
    m.put("allowWithdrawal", p.isAllowWithdrawal());
    m.put("version", p.getVersion());
    return json.writeValueAsString(m);
  }

  private ScholarshipException bad(String c, String m) {
    return new ScholarshipException(HttpStatus.BAD_REQUEST, c, m);
  }

  private ScholarshipException conflict(String c, String m) {
    return new ScholarshipException(HttpStatus.CONFLICT, c, m);
  }

  private ScholarshipException notFound(String c, String m) {
    return new ScholarshipException(HttpStatus.NOT_FOUND, c, m);
  }
}
