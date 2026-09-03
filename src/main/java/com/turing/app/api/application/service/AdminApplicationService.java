package com.turing.app.api.application.service;

import com.turing.app.api.application.dto.AdminApplicationDtos.*;
import com.turing.app.api.application.entity.*;
import com.turing.app.api.application.exception.ApplicationException;
import com.turing.app.api.application.repository.*;
import com.turing.app.api.audit.service.AuditService;
import com.turing.app.api.document.entity.FileStatus;
import com.turing.app.api.document.repository.StoredFileRepository;
import com.turing.app.api.user.entity.User;
import com.turing.app.api.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class AdminApplicationService {
  private static final Map<ApplicationStatus, Set<ApplicationStatus>> TRANSITIONS =
      Map.of(
          ApplicationStatus.SUBMITTED,
              Set.of(
                  ApplicationStatus.MISSING_DOCUMENT,
                  ApplicationStatus.APPROVED,
                  ApplicationStatus.REJECTED),
          ApplicationStatus.MISSING_DOCUMENT,
              Set.of(
                  ApplicationStatus.SUBMITTED,
                  ApplicationStatus.APPROVED,
                  ApplicationStatus.REJECTED),
          ApplicationStatus.UNDER_REVIEW,
              Set.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED),
          ApplicationStatus.SHORTLISTED,
              Set.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED),
          ApplicationStatus.INTERVIEW,
              Set.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED),
          ApplicationStatus.WAITLISTED,
              Set.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED));
  private static final Map<String, String> SORTS =
      Map.of(
          "createdAt",
          "createdAt",
          "submittedAt",
          "submittedAt",
          "status",
          "status",
          "completion",
          "completion");
  private final ApplicationRepository applications;
  private final ApplicationAnswerRepository answers;
  private final StoredFileRepository files;
  private final ApplicationNoteRepository notes;
  private final ApplicationStatusHistoryRepository history;
  private final UserRepository users;
  private final AuditService audit;
  private final ObjectMapper mapper;
  private final Clock clock;

  public AdminApplicationService(
      ApplicationRepository applications,
      ApplicationAnswerRepository answers,
      StoredFileRepository files,
      ApplicationNoteRepository notes,
      ApplicationStatusHistoryRepository history,
      UserRepository users,
      AuditService audit,
      ObjectMapper mapper,
      Clock clock) {
    this.applications = applications;
    this.answers = answers;
    this.files = files;
    this.notes = notes;
    this.history = history;
    this.users = users;
    this.audit = audit;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResponse<Summary> list(
      String search,
      UUID periodId,
      UUID programId,
      ApplicationStatus status,
      int page,
      int size,
      String sort,
      String direction) {
    if (page < 0 || size < 1 || size > 100)
      throw bad("INVALID_PAGE", "Sayfalama değerlerini kontrol edin.");
    String property = SORTS.get(sort);
    if (property == null) throw bad("INVALID_SORT", "Sıralama alanına izin verilmiyor.");
    Sort.Direction order =
        "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Specification<Application> spec =
        (root, query, cb) -> {
          List<jakarta.persistence.criteria.Predicate> values = new ArrayList<>();
          if (periodId != null) values.add(cb.equal(root.get("period").get("id"), periodId));
          if (programId != null)
            values.add(cb.equal(root.get("period").get("program").get("id"), programId));
          if (status != null) values.add(root.get("status").in(statusGroup(status)));
          if (search != null && !search.isBlank()) {
            String q = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            values.add(
                cb.or(
                    cb.like(cb.lower(root.get("profile").get("user").get("email")), q),
                    cb.like(cb.lower(root.get("profile").get("user").get("firstName")), q),
                    cb.like(cb.lower(root.get("profile").get("user").get("lastName")), q)));
          }
          return cb.and(values.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    Page<Application> result =
        applications.findAll(spec, PageRequest.of(page, size, Sort.by(order, property)));
    return new PageResponse<>(
        result.getContent().stream().map(this::summary).toList(),
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages());
  }

  private Set<ApplicationStatus> statusGroup(ApplicationStatus status) {
    return switch (status) {
      case SUBMITTED ->
          Set.of(
              ApplicationStatus.DRAFT,
              ApplicationStatus.SUBMITTED,
              ApplicationStatus.UNDER_REVIEW,
              ApplicationStatus.SHORTLISTED,
              ApplicationStatus.INTERVIEW,
              ApplicationStatus.WAITLISTED);
      case REJECTED -> Set.of(ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN);
      default -> Set.of(status);
    };
  }

  @Transactional(readOnly = true)
  public Detail detail(UUID id) {
    Application app = find(id);
    return detail(app);
  }

  @Transactional
  public Note saveNote(UUID actorId, UUID id, NoteRequest request, String ip) {
    Application app = find(id);
    User actor = findUser(actorId);
    ApplicationNote saved =
        notes
            .findFirstByApplicationIdOrderByCreatedAtDesc(id)
            .map(
                value -> {
                  value.update(actor, request.content().trim(), clock.instant());
                  return value;
                })
            .orElseGet(
                () ->
                    notes.save(
                        ApplicationNote.create(
                            app, actor, request.content().trim(), clock.instant())));
    notes.flush();
    audit.record(
        actorId,
        "APPLICATION_NOTE_SAVED",
        "APPLICATION",
        id,
        "{}",
        json(Map.of("noteId", saved.getId(), "visibility", "INTERNAL")),
        ip);
    return note(saved);
  }

  @Transactional
  public Detail changeStatus(UUID actorId, UUID id, StatusRequest request, String ip) {
    Application app = find(id);
    if (app.getVersion() != request.version())
      throw new ApplicationException(
          HttpStatus.CONFLICT,
          "VERSION_CONFLICT",
          "Kayıt başka bir işlem tarafından güncellendi. Sayfayı yenileyin.");
    ApplicationStatus old = app.getStatus();
    if (!TRANSITIONS.getOrDefault(old, Set.of()).contains(request.status()))
      throw new ApplicationException(
          HttpStatus.CONFLICT,
          "INVALID_APPLICATION_TRANSITION",
          "Bu başvuru durum geçişine izin verilmiyor.");
    User actor = findUser(actorId);
    app.changeStatus(request.status(), clock.instant());
    history.save(
        ApplicationStatusHistory.create(
            app, old, request.status(), actor, request.reason().trim(), clock.instant()));
    applications.flush();
    audit.record(
        actorId,
        "APPLICATION_STATUS_CHANGED",
        "APPLICATION",
        id,
        json(Map.of("status", old)),
        json(Map.of("status", request.status(), "reason", request.reason().trim())),
        ip);
    return detail(app);
  }

  private Detail detail(Application app) {
    List<Answer> answerValues =
        answers.findByApplicationIdOrderByFieldId(app.getId()).stream()
            .map(
                value ->
                    new Answer(
                        value.getField().getId(), value.getField().getLabel(), value.getValue()))
            .toList();
    List<Document> documents =
        files
            .findByApplicationIdAndStatusOrderByRequirementDisplayOrderAsc(
                app.getId(), FileStatus.ACTIVE)
            .stream()
            .map(
                value ->
                    new Document(
                        value.getId(),
                        value.getRequirement().getName(),
                        value.getOriginalName(),
                        value.getMimeType(),
                        value.getSizeBytes(),
                        value.getUploadedAt()))
            .toList();
    return new Detail(
        summary(app),
        answerValues,
        documents,
        notes.findByApplicationIdOrderByCreatedAtDesc(app.getId()).stream()
            .map(this::note)
            .toList(),
        history.findByApplicationIdOrderByCreatedAtDesc(app.getId()).stream()
            .map(
                value ->
                    new History(
                        value.getOldStatus(),
                        value.getNewStatus(),
                        name(value.getChangedBy()),
                        value.getReason(),
                        value.getCreatedAt()))
            .toList());
  }

  private Summary summary(Application app) {
    User user = app.getProfile().getUser();
    return new Summary(
        app.getId(),
        user.getId(),
        name(user),
        user.getEmail(),
        Optional.ofNullable(app.getProfile().getUniversity())
            .map(value -> value.getName())
            .orElse(app.getProfile().getOtherUniversity()),
        Optional.ofNullable(app.getProfile().getDepartment())
            .map(value -> value.getName())
            .orElse(app.getProfile().getOtherDepartment()),
        app.getPeriod().getId(),
        app.getPeriod().getProgram().getId(),
        app.getPeriod().getName(),
        app.getPeriod().getProgram().getName(),
        app.getStatus(),
        app.getCompletion(),
        app.getSubmittedAt(),
        app.getCreatedAt(),
        app.getVersion());
  }

  private Note note(ApplicationNote value) {
    return new Note(
        value.getId(),
        name(value.getAdmin()),
        value.getContent(),
        value.getCreatedAt(),
        value.getVersion());
  }

  private String name(User user) {
    return user.getFirstName() + " " + user.getLastName();
  }

  private Application find(UUID id) {
    return applications
        .findById(id)
        .orElseThrow(
            () ->
                new ApplicationException(
                    HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND", "Başvuru bulunamadı."));
  }

  private User findUser(UUID id) {
    return users
        .findById(id)
        .orElseThrow(
            () ->
                new ApplicationException(
                    HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Kullanıcı bulunamadı."));
  }

  private ApplicationException bad(String code, String message) {
    return new ApplicationException(HttpStatus.BAD_REQUEST, code, message);
  }

  private String json(Object value) {
    return mapper.writeValueAsString(value);
  }
}
