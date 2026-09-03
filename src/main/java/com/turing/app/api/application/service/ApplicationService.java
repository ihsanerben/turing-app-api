package com.turing.app.api.application.service;

import com.turing.app.api.application.dto.*;
import com.turing.app.api.application.entity.*;
import com.turing.app.api.application.exception.ApplicationException;
import com.turing.app.api.application.repository.*;
import com.turing.app.api.audit.service.AuditService;
import com.turing.app.api.document.service.DocumentPolicyService;
import com.turing.app.api.profile.entity.StudentProfile;
import com.turing.app.api.profile.repository.StudentProfileRepository;
import com.turing.app.api.scholarship.dto.FormResponse;
import com.turing.app.api.scholarship.entity.*;
import com.turing.app.api.scholarship.repository.*;
import com.turing.app.api.user.entity.User;
import com.turing.app.api.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {
  private final ApplicationRepository applications;
  private final ApplicationAnswerRepository answers;
  private final ApplicationSnapshotRepository snapshots;
  private final ApplicationStatusHistoryRepository history;
  private final StudentProfileRepository profiles;
  private final ApplicationPeriodRepository periods;
  private final FormDefinitionRepository forms;
  private final UserRepository users;
  private final DocumentPolicyService documents;
  private final AuditService audit;
  private final Clock clock;

  public ApplicationService(
      ApplicationRepository applications,
      ApplicationAnswerRepository answers,
      ApplicationSnapshotRepository snapshots,
      ApplicationStatusHistoryRepository history,
      StudentProfileRepository profiles,
      ApplicationPeriodRepository periods,
      FormDefinitionRepository forms,
      UserRepository users,
      DocumentPolicyService documents,
      AuditService audit,
      Clock clock) {
    this.applications = applications;
    this.answers = answers;
    this.snapshots = snapshots;
    this.history = history;
    this.profiles = profiles;
    this.periods = periods;
    this.forms = forms;
    this.users = users;
    this.documents = documents;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<ApplicationResponse> list(UUID userId) {
    return applications.findByProfileUserIdOrderByCreatedAtDesc(userId).stream()
        .map(ApplicationResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public ApplicationResponse get(UUID userId, UUID id) {
    return ApplicationResponse.from(findOwned(userId, id));
  }

  @Transactional(readOnly = true)
  public ApplicationFormResponse form(UUID userId, UUID id) {
    Application app = findOwned(userId, id);
    return response(app);
  }

  @Transactional
  public ApplicationResponse create(UUID userId, ApplicationCreateRequest request) {
    Instant now = clock.instant();
    ApplicationPeriod period =
        periods
            .findById(request.periodId())
            .orElseThrow(() -> notFound("PERIOD_NOT_FOUND", "Başvuru dönemi bulunamadı."));
    ensureOpen(period, now);
    StudentProfile profile =
        profiles
            .findByUserId(userId)
            .orElseThrow(
                () -> bad("PROFILE_REQUIRED", "Başvuru oluşturmadan önce profilinizi kaydedin."));
    if (applications.existsByProfileIdAndPeriodId(profile.getId(), period.getId()))
      throw conflict("APPLICATION_ALREADY_EXISTS", "Bu dönem için zaten bir başvurunuz var.");
    FormDefinition form =
        forms
            .findByPeriodIdAndStatus(period.getId(), FormStatus.PUBLISHED)
            .orElseThrow(
                () ->
                    conflict(
                        "PUBLISHED_FORM_REQUIRED", "Bu dönem için yayınlanmış form bulunmuyor."));
    User actor = user(userId);
    try {
      Application saved = applications.saveAndFlush(Application.draft(profile, period, form, now));
      history.save(
          ApplicationStatusHistory.create(
              saved, null, ApplicationStatus.DRAFT, actor, "Başvuru taslağı oluşturuldu.", now));
      return ApplicationResponse.from(saved);
    } catch (DataIntegrityViolationException exception) {
      throw conflict("APPLICATION_ALREADY_EXISTS", "Bu dönem için zaten bir başvurunuz var.");
    }
  }

  @Transactional
  public ApplicationFormResponse saveAnswers(UUID userId, UUID id, AnswersUpdateRequest request) {
    Application app = findOwned(userId, id);
    ensureStudentEditable(app, clock.instant());
    checkVersion(app.getVersion(), request.version());
    Map<UUID, FormField> fields = fields(app.getForm());
    Set<UUID> seen = new HashSet<>();
    List<ApplicationAnswer> values = new ArrayList<>();
    for (AnswerRequest item : request.answers()) {
      if (!seen.add(item.fieldId()))
        throw bad("DUPLICATE_ANSWER", "Bir alan için birden fazla cevap gönderilemez.");
      FormField field = fields.get(item.fieldId());
      if (field == null) throw bad("FIELD_NOT_IN_FORM", "Cevap alanı bu form sürümüne ait değil.");
      values.add(toAnswer(app, field, item.value()));
    }
    answers.deleteByApplicationId(app.getId());
    answers.flush();
    answers.saveAll(values);
    int answerable =
        (int)
            fields.values().stream().filter(field -> field.getType() != FormFieldType.FILE).count();
    int completion = answerable == 0 ? 100 : (int) Math.floor(values.size() * 100.0 / answerable);
    app.answersChanged(completion, clock.instant());
    applications.flush();
    return response(app);
  }

  @Transactional
  public ApplicationResponse submit(
      UUID userId, UUID id, ApplicationVersionRequest request, String ipAddress) {
    Application app = findOwned(userId, id);
    Instant now = clock.instant();
    boolean correction = app.getStatus() == ApplicationStatus.MISSING_DOCUMENT;
    boolean update = app.getStatus() == ApplicationStatus.SUBMITTED;
    ensureStudentEditable(app, now);
    checkVersion(app.getVersion(), request.version());
    List<ApplicationAnswer> savedAnswers = answers.findByApplicationIdOrderByFieldId(id);
    Map<UUID, ApplicationAnswer> byField = new HashMap<>();
    savedAnswers.forEach(answer -> byField.put(answer.getField().getId(), answer));
    List<String> missing =
        fields(app.getForm()).values().stream()
            .filter(
                field ->
                    field.isRequired()
                        && field.getType() != FormFieldType.FILE
                        && !byField.containsKey(field.getId()))
            .map(FormField::getLabel)
            .toList();
    if (!missing.isEmpty())
      throw bad(
          "REQUIRED_ANSWERS_MISSING", "Zorunlu alanları tamamlayın: " + String.join(", ", missing));
    List<String> missingDocuments = documents.missingRequired(app);
    if (!missingDocuments.isEmpty())
      throw bad(
          "REQUIRED_DOCUMENTS_MISSING",
          "Zorunlu belgeleri yükleyin: " + String.join(", ", missingDocuments));
    if (update) {
      app.updated(now);
      audit.record(
          userId,
          "APPLICATION_UPDATED_BY_STUDENT",
          "APPLICATION",
          app.getId(),
          "{\"status\":\"SUBMITTED\"}",
          "{\"status\":\"SUBMITTED\",\"answersAndDocumentsUpdated\":true}",
          ipAddress);
    } else if (correction) {
      app.resubmit(now);
    } else {
      if (snapshots.existsByApplicationId(id))
        throw conflict("APPLICATION_ALREADY_SUBMITTED", "Başvuru daha önce gönderilmiş.");
      snapshots.save(
          ApplicationSnapshot.create(
              app, app.getForm().getVersionNumber(), profileSnapshot(app.getProfile()), now));
      app.submit(now);
    }
    if (!update) {
      ApplicationStatus old =
          correction ? ApplicationStatus.MISSING_DOCUMENT : ApplicationStatus.DRAFT;
      history.save(
          ApplicationStatusHistory.create(
              app,
              old,
              ApplicationStatus.SUBMITTED,
              user(userId),
              correction
                  ? "Eksik belgeler tamamlanarak başvuru yeniden gönderildi."
                  : "Başvuru gönderildi.",
              now));
    }
    applications.flush();
    return ApplicationResponse.from(app);
  }

  @Transactional
  public ApplicationResponse withdraw(UUID userId, UUID id, ApplicationVersionRequest request) {
    Application app = findOwned(userId, id);
    checkVersion(app.getVersion(), request.version());
    if (app.getStatus() != ApplicationStatus.DRAFT
        && app.getStatus() != ApplicationStatus.SUBMITTED)
      throw conflict("APPLICATION_NOT_WITHDRAWABLE", "Bu durumdaki başvuru geri çekilemez.");
    Instant now = clock.instant();
    if (!app.getPeriod().isAllowWithdrawal() || !now.isBefore(app.getPeriod().getEndsAt()))
      throw conflict("WITHDRAWAL_NOT_ALLOWED", "Bu dönem için geri çekme süresi sona erdi.");
    ApplicationStatus old = app.getStatus();
    app.withdraw(now);
    history.save(
        ApplicationStatusHistory.create(
            app,
            old,
            ApplicationStatus.WITHDRAWN,
            user(userId),
            "Başvuru öğrenci tarafından geri çekildi.",
            now));
    applications.flush();
    return ApplicationResponse.from(app);
  }

  private ApplicationAnswer toAnswer(Application app, FormField field, Object raw) {
    Instant now = clock.instant();
    return switch (field.getType()) {
      case TEXT, TEXTAREA, EMAIL, PHONE, SELECT, RADIO ->
          ApplicationAnswer.text(app, field, validatedText(field, raw), now);
      case INTEGER, DECIMAL ->
          ApplicationAnswer.number(app, field, validatedNumber(field, raw), now);
      case BOOLEAN, CHECKBOX ->
          ApplicationAnswer.bool(app, field, validatedBoolean(field, raw), now);
      case DATE -> ApplicationAnswer.date(app, field, validatedDate(field, raw), now);
      case MULTI_SELECT ->
          ApplicationAnswer.multiple(app, field, validatedMultiple(field, raw), now);
      case FILE ->
          throw bad(
              "FILE_ANSWER_NOT_SUPPORTED", "Dosya alanları belge yükleme ekranından yönetilir.");
    };
  }

  private String validatedText(FormField field, Object raw) {
    if (!(raw instanceof String value) || value.isBlank()) throw invalid(field);
    String text = value.trim();
    Map<String, Object> rules = field.getValidationRules();
    Number min = (Number) rules.get("minLength"), max = (Number) rules.get("maxLength");
    if (min != null && text.length() < min.intValue()
        || max != null && text.length() > max.intValue()) throw invalid(field);
    Object pattern = rules.get("pattern");
    if (pattern instanceof String regex && !Pattern.matches(regex, text)) throw invalid(field);
    if ((field.getType() == FormFieldType.SELECT || field.getType() == FormFieldType.RADIO)
        && field.getOptions().stream().noneMatch(option -> option.getValue().equals(text)))
      throw invalid(field);
    if (field.getType() == FormFieldType.EMAIL && !text.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
      throw invalid(field);
    return text;
  }

  private BigDecimal validatedNumber(FormField field, Object raw) {
    BigDecimal value;
    try {
      value = new BigDecimal(raw.toString());
    } catch (Exception exception) {
      throw invalid(field);
    }
    if (field.getType() == FormFieldType.INTEGER && value.stripTrailingZeros().scale() > 0)
      throw invalid(field);
    Number min = (Number) field.getValidationRules().get("min"),
        max = (Number) field.getValidationRules().get("max");
    if (min != null && value.compareTo(new BigDecimal(min.toString())) < 0
        || max != null && value.compareTo(new BigDecimal(max.toString())) > 0) throw invalid(field);
    return value;
  }

  private Boolean validatedBoolean(FormField field, Object raw) {
    if (!(raw instanceof Boolean value)
        || field.getType() == FormFieldType.CHECKBOX && field.isRequired() && !value)
      throw invalid(field);
    return value;
  }

  private LocalDate validatedDate(FormField field, Object raw) {
    try {
      return LocalDate.parse(raw.toString());
    } catch (Exception exception) {
      throw invalid(field);
    }
  }

  private List<String> validatedMultiple(FormField field, Object raw) {
    if (!(raw instanceof List<?> list)
        || list.isEmpty()
        || list.stream().anyMatch(value -> !(value instanceof String))) throw invalid(field);
    List<String> values = list.stream().map(String.class::cast).distinct().toList();
    Set<String> allowed =
        new HashSet<>(field.getOptions().stream().map(option -> option.getValue()).toList());
    if (values.size() != list.size() || !allowed.containsAll(values)) throw invalid(field);
    return values;
  }

  private Map<UUID, FormField> fields(FormDefinition form) {
    Map<UUID, FormField> values = new LinkedHashMap<>();
    form.getSections()
        .forEach(section -> section.getFields().forEach(field -> values.put(field.getId(), field)));
    return values;
  }

  private Map<String, Object> profileSnapshot(StudentProfile profile) {
    User owner = profile.getUser();
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("userId", owner.getId().toString());
    value.put("email", owner.getEmail());
    value.put("firstName", owner.getFirstName());
    value.put("lastName", owner.getLastName());
    value.put("birthDate", string(profile.getBirthDate()));
    value.put("phone", profile.getPhone());
    value.put("addressLine", profile.getAddressLine());
    value.put("city", profile.getCity());
    value.put("postalCode", profile.getPostalCode());
    value.put("countryCode", profile.getCountryCode());
    value.put(
        "university",
        profile.getUniversity() == null
            ? profile.getOtherUniversity()
            : profile.getUniversity().getName());
    value.put(
        "department",
        profile.getDepartment() == null
            ? profile.getOtherDepartment()
            : profile.getDepartment().getName());
    value.put("educationLevel", string(profile.getEducationLevel()));
    value.put("studyYear", profile.getStudyYear());
    value.put("gpa", profile.getGpa());
    value.put("profileVersion", profile.getVersion());
    return value;
  }

  private String string(Object value) {
    return value == null ? null : value.toString();
  }

  private ApplicationFormResponse response(Application app) {
    return new ApplicationFormResponse(
        ApplicationResponse.from(app),
        FormResponse.from(app.getForm()),
        answers.findByApplicationIdOrderByFieldId(app.getId()).stream()
            .map(AnswerResponse::from)
            .toList());
  }

  private void ensureOpen(ApplicationPeriod period, Instant now) {
    if (period.getStatus() != PeriodStatus.OPEN
        || now.isBefore(period.getStartsAt())
        || !now.isBefore(period.getEndsAt()))
      throw conflict("APPLICATION_PERIOD_CLOSED", "Başvuru dönemi açık değil.");
  }

  private void ensureStudentEditable(Application app, Instant now) {
    if (app.getStatus() != ApplicationStatus.DRAFT
        && app.getStatus() != ApplicationStatus.SUBMITTED
        && app.getStatus() != ApplicationStatus.MISSING_DOCUMENT)
      throw conflict(
          "APPLICATION_IMMUTABLE", "Sonuçlanmış veya geri çekilmiş başvuru değiştirilemez.");
    ensureOpen(app.getPeriod(), now);
  }

  private Application findOwned(UUID userId, UUID id) {
    return applications
        .findByIdAndProfileUserId(id, userId)
        .orElseThrow(() -> notFound("APPLICATION_NOT_FOUND", "Başvuru bulunamadı."));
  }

  private User user(UUID id) {
    return users
        .findById(id)
        .orElseThrow(() -> notFound("USER_NOT_FOUND", "Kullanıcı bulunamadı."));
  }

  private void checkVersion(long current, Long requested) {
    if (requested == null || current != requested)
      throw conflict(
          "VERSION_CONFLICT", "Başvuru başka bir işlem tarafından güncellendi. Sayfayı yenileyin.");
  }

  private ApplicationException invalid(FormField field) {
    return bad("INVALID_ANSWER", "Geçersiz cevap: " + field.getLabel());
  }

  private ApplicationException bad(String code, String message) {
    return new ApplicationException(HttpStatus.BAD_REQUEST, code, message);
  }

  private ApplicationException conflict(String code, String message) {
    return new ApplicationException(HttpStatus.CONFLICT, code, message);
  }

  private ApplicationException notFound(String code, String message) {
    return new ApplicationException(HttpStatus.NOT_FOUND, code, message);
  }
}
