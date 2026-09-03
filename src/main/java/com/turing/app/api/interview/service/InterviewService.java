package com.turing.app.api.interview.service;

import com.turing.app.api.application.entity.*;
import com.turing.app.api.application.repository.ApplicationRepository;
import com.turing.app.api.audience.service.AudienceListService;
import com.turing.app.api.audit.service.AuditService;
import com.turing.app.api.interview.dto.*;
import com.turing.app.api.interview.dto.AdminInterviewResponse.Feedback;
import com.turing.app.api.interview.entity.*;
import com.turing.app.api.interview.exception.InterviewException;
import com.turing.app.api.interview.repository.*;
import com.turing.app.api.notification.mail.EmailSender;
import com.turing.app.api.notification.service.NotificationService;
import com.turing.app.api.user.entity.User;
import com.turing.app.api.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterviewService {
  private static final Logger log = LoggerFactory.getLogger(InterviewService.class);
  private static final Map<InterviewStatus, Set<InterviewStatus>> TRANSITIONS =
      Map.of(
          InterviewStatus.SCHEDULED,
          Set.of(
              InterviewStatus.COMPLETED,
              InterviewStatus.CANCELLED,
              InterviewStatus.NO_SHOW,
              InterviewStatus.RESCHEDULED),
          InterviewStatus.RESCHEDULED,
          Set.of(InterviewStatus.SCHEDULED, InterviewStatus.CANCELLED));
  private final InterviewRepository interviews;
  private final InterviewFeedbackRepository feedback;
  private final ApplicationRepository applications;
  private final UserRepository users;
  private final AuditService audit;
  private final Clock clock;
  private final AudienceListService audienceLists;
  private final EmailSender emailSender;
  private final NotificationService notifications;

  public InterviewService(
      InterviewRepository interviews,
      InterviewFeedbackRepository feedback,
      ApplicationRepository applications,
      UserRepository users,
      AuditService audit,
      Clock clock,
      AudienceListService audienceLists,
      EmailSender emailSender,
      NotificationService notifications) {
    this.interviews = interviews;
    this.feedback = feedback;
    this.applications = applications;
    this.users = users;
    this.audit = audit;
    this.clock = clock;
    this.audienceLists = audienceLists;
    this.emailSender = emailSender;
    this.notifications = notifications;
  }

  @Transactional(readOnly = true)
  public List<StudentInterviewResponse> mine(UUID userId) {
    return interviews.findByApplicationProfileUserIdOrderByStartsAtDesc(userId).stream()
        .map(StudentInterviewResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public StudentInterviewResponse mine(UUID userId, UUID id) {
    return StudentInterviewResponse.from(
        interviews.findByIdAndApplicationProfileUserId(id, userId).orElseThrow(() -> notFound()));
  }

  @Transactional(readOnly = true)
  public List<AdminInterviewResponse> byApplication(UUID applicationId) {
    application(applicationId);
    return interviews.findByApplicationIdOrderByStartsAtDesc(applicationId).stream()
        .map(this::adminResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AdminInterviewResponse> all() {
    return interviews.findAllByOrderByStartsAtDesc().stream().map(this::adminResponse).toList();
  }

  @Transactional
  public AdminInterviewResponse create(
      UUID actorId, UUID applicationId, InterviewRequest r, String ip) {
    Application app = application(applicationId);
    if (app.getStatus() != ApplicationStatus.SHORTLISTED
        && app.getStatus() != ApplicationStatus.INTERVIEW
        && app.getStatus() != ApplicationStatus.APPROVED)
      throw conflict(
          "APPLICATION_NOT_INTERVIEWABLE",
          "Yalnız olumlu, kısa liste veya mülakat durumundaki başvuruya mülakat planlanabilir.");
    validate(r);
    User actor = user(actorId);
    Interview saved =
        interviews.save(
            Interview.create(
                app,
                actor,
                r.startsAt(),
                r.endsAt(),
                r.locationType(),
                clean(r.location()),
                clean(r.meetingUrl()),
                clock.instant()));
    interviews.flush();
    audit.record(
        actorId, "INTERVIEW_SCHEDULED", "INTERVIEW", saved.getId(), "{}", scheduleJson(saved), ip);
    User student = app.getProfile().getUser();
    String message =
        "Mülakatınız "
            + r.startsAt()
            + " tarihinde planlandı. Detayları hesabınızdan görebilirsiniz.";
    notifications.create(
        student.getId(), "Mülakatınız planlandı", message, "INFO", "INTERVIEW", saved.getId());
    try {
      emailSender.send(student.getEmail(), "Mülakatınız planlandı", message);
    } catch (RuntimeException exception) {
      log.warn("Interview email could not be sent interviewId={}", saved.getId(), exception);
    }
    return adminResponse(saved);
  }

  @Transactional
  public List<AdminInterviewResponse> createBulk(
      UUID actorId, BulkInterviewRequest request, String ip) {
    InterviewRequest schedule =
        new InterviewRequest(
            request.startsAt(),
            request.endsAt(),
            request.locationType(),
            request.location(),
            request.meetingUrl(),
            null);
    return audienceLists.entity(request.listId()).getApplications().stream()
        .map(application -> create(actorId, application.getId(), schedule, ip))
        .toList();
  }

  @Transactional
  public AdminInterviewResponse update(UUID actorId, UUID id, InterviewRequest r, String ip) {
    Interview v = interview(id);
    check(v.getVersion(), r.version());
    if (v.getStatus() != InterviewStatus.SCHEDULED && v.getStatus() != InterviewStatus.RESCHEDULED)
      throw conflict(
          "INTERVIEW_IMMUTABLE",
          "Tamamlanmış, iptal edilmiş veya katılım sağlanmamış mülakat değiştirilemez.");
    validate(r);
    String old = scheduleJson(v);
    v.updateSchedule(
        r.startsAt(),
        r.endsAt(),
        r.locationType(),
        clean(r.location()),
        clean(r.meetingUrl()),
        clock.instant());
    interviews.flush();
    audit.record(actorId, "INTERVIEW_UPDATED", "INTERVIEW", id, old, scheduleJson(v), ip);
    return adminResponse(v);
  }

  @Transactional
  public AdminInterviewResponse transition(
      UUID actorId, UUID id, InterviewStatusRequest r, String ip) {
    Interview v = interview(id);
    check(v.getVersion(), r.version());
    if (!TRANSITIONS.getOrDefault(v.getStatus(), Set.of()).contains(r.status()))
      throw conflict("INVALID_INTERVIEW_TRANSITION", "Bu mülakat durum geçişine izin verilmiyor.");
    InterviewStatus old = v.getStatus();
    v.transition(r.status(), clock.instant());
    interviews.flush();
    audit.record(
        actorId,
        "INTERVIEW_STATUS_CHANGED",
        "INTERVIEW",
        id,
        "{\"status\":\"" + old + "\"}",
        "{\"status\":\"" + r.status() + "\"}",
        ip);
    return adminResponse(v);
  }

  @Transactional
  public AdminInterviewResponse upsertFeedback(
      UUID actorId, UUID id, FeedbackRequest r, String ip) {
    Interview interview = interview(id);
    if (interview.getStatus() != InterviewStatus.COMPLETED)
      throw conflict(
          "INTERVIEW_NOT_COMPLETED", "Feedback yalnız tamamlanan mülakat için kaydedilebilir.");
    User actor = user(actorId);
    Optional<InterviewFeedback> found = feedback.findByInterviewIdAndInterviewerId(id, actorId);
    InterviewFeedback value;
    if (found.isPresent()) {
      value = found.get();
      check(value.getVersion(), r.version());
      value.update(r.score(), r.notes().trim(), clean(r.recommendation()), clock.instant());
    } else {
      if (r.version() != null && r.version() != 0) throw version();
      value =
          feedback.save(
              InterviewFeedback.create(
                  interview,
                  actor,
                  r.score(),
                  r.notes().trim(),
                  clean(r.recommendation()),
                  clock.instant()));
    }
    try {
      feedback.flush();
      audit.record(
          actorId,
          "INTERVIEW_FEEDBACK_UPSERTED",
          "INTERVIEW",
          id,
          "{}",
          "{\"interviewerId\":\"" + actorId + "\"}",
          ip);
      return adminResponse(interview);
    } catch (DataIntegrityViolationException e) {
      throw conflict("FEEDBACK_CONFLICT", "Bu değerlendiricinin feedback kaydı zaten var.");
    }
  }

  private AdminInterviewResponse adminResponse(Interview v) {
    List<Feedback> values =
        feedback.findByInterviewIdOrderByCreatedAtAsc(v.getId()).stream()
            .map(
                f ->
                    new Feedback(
                        f.getId(),
                        f.getInterviewer().getId(),
                        f.getInterviewer().getFirstName() + " " + f.getInterviewer().getLastName(),
                        f.getScore(),
                        f.getNotes(),
                        f.getRecommendation(),
                        f.getVersion()))
            .toList();
    return new AdminInterviewResponse(
        v.getId(),
        v.getApplication().getId(),
        v.getApplication().getProfile().getUser().getFirstName()
            + " "
            + v.getApplication().getProfile().getUser().getLastName(),
        v.getApplication().getPeriod().getProgram().getName(),
        v.getStartsAt(),
        v.getEndsAt(),
        v.getStatus(),
        v.getLocationType(),
        v.getLocation(),
        v.getMeetingUrl(),
        v.getCreatedBy().getFirstName() + " " + v.getCreatedBy().getLastName(),
        v.getVersion(),
        values);
  }

  private void validate(InterviewRequest r) {
    if (!r.endsAt().isAfter(r.startsAt()))
      throw bad("INVALID_INTERVIEW_TIME", "Mülakat bitişi başlangıçtan sonra olmalıdır.");
    if (r.locationType() == InterviewLocationType.ONLINE && clean(r.meetingUrl()) == null)
      throw bad("MEETING_URL_REQUIRED", "Online mülakat için bağlantı zorunludur.");
    if (r.locationType() == InterviewLocationType.IN_PERSON && clean(r.location()) == null)
      throw bad("LOCATION_REQUIRED", "Yüz yüze mülakat için konum zorunludur.");
  }

  private Application application(UUID id) {
    return applications
        .findById(id)
        .orElseThrow(
            () ->
                new InterviewException(
                    HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND", "Başvuru bulunamadı."));
  }

  private Interview interview(UUID id) {
    return interviews.findById(id).orElseThrow(this::notFound);
  }

  private User user(UUID id) {
    return users
        .findById(id)
        .orElseThrow(
            () ->
                new InterviewException(
                    HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND", "Admin bulunamadı."));
  }

  private InterviewException notFound() {
    return new InterviewException(
        HttpStatus.NOT_FOUND, "INTERVIEW_NOT_FOUND", "Mülakat bulunamadı.");
  }

  private void check(long actual, Long supplied) {
    if (supplied == null || actual != supplied) throw version();
  }

  private InterviewException version() {
    return conflict(
        "INTERVIEW_VERSION_CONFLICT",
        "Kayıt başka bir işlem tarafından güncellendi. Sayfayı yenileyin.");
  }

  private String clean(String v) {
    return v == null || v.isBlank() ? null : v.trim();
  }

  private String scheduleJson(Interview v) {
    return "{\"startsAt\":\""
        + v.getStartsAt()
        + "\",\"endsAt\":\""
        + v.getEndsAt()
        + "\",\"locationType\":\""
        + v.getLocationType()
        + "\"}";
  }

  private InterviewException bad(String c, String m) {
    return new InterviewException(HttpStatus.BAD_REQUEST, c, m);
  }

  private InterviewException conflict(String c, String m) {
    return new InterviewException(HttpStatus.CONFLICT, c, m);
  }
}
