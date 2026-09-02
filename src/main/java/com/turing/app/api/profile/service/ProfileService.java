package com.turing.app.api.profile.service;

import com.turing.app.api.audit.service.AuditService;
import com.turing.app.api.profile.dto.*;
import com.turing.app.api.profile.entity.*;
import com.turing.app.api.profile.exception.ProfileException;
import com.turing.app.api.profile.repository.*;
import com.turing.app.api.profile.security.NationalIdEncryptionService;
import com.turing.app.api.user.entity.User;
import com.turing.app.api.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProfileService {
  private final StudentProfileRepository profiles;
  private final UserRepository users;
  private final UniversityRepository universities;
  private final DepartmentRepository departments;
  private final NationalIdEncryptionService encryption;
  private final AuditService audit;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public ProfileService(
      StudentProfileRepository profiles,
      UserRepository users,
      UniversityRepository universities,
      DepartmentRepository departments,
      NationalIdEncryptionService encryption,
      AuditService audit,
      ObjectMapper objectMapper,
      Clock clock) {
    this.profiles = profiles;
    this.users = users;
    this.universities = universities;
    this.departments = departments;
    this.encryption = encryption;
    this.audit = audit;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ProfileResponse get(UUID userId) {
    return profiles.findByUserId(userId).map(this::response).orElseGet(() -> empty(userId));
  }

  @Transactional
  public ProfileResponse updateOwn(UUID userId, ProfileUpdateRequest request) {
    return update(userId, request, null, null);
  }

  @Transactional
  public ProfileResponse updateByAdmin(
      UUID actorId, UUID userId, ProfileUpdateRequest request, String ip) {
    return update(userId, request, actorId, ip);
  }

  private ProfileResponse update(
      UUID userId, ProfileUpdateRequest request, UUID adminActorId, String ip) {
    User user =
        users
            .findById(userId)
            .orElseThrow(
                () -> error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Kullanıcı bulunamadı."));
    StudentProfile profile = profiles.findByUserId(userId).orElse(null);
    if (profile == null) {
      if (request.version() != null && request.version() != 0) throw versionConflict();
      profile = StudentProfile.create(user, clock.instant());
    } else if (request.version() == null || profile.getVersion() != request.version()) {
      throw versionConflict();
    }
    String before = auditSnapshot(profile);
    University university = selectedUniversity(request);
    Department department = selectedDepartment(request, university);
    String otherUniversity = clean(request.otherUniversity());
    String otherDepartment = clean(request.otherDepartment());
    validateFallbacks(
        request.universityId(), request.departmentId(), otherUniversity, otherDepartment);
    profile.update(
        encryption.encrypt(clean(request.nationalId())),
        request.birthDate(),
        clean(request.phone()),
        clean(request.addressLine()),
        clean(request.city()),
        clean(request.postalCode()),
        upper(request.countryCode()),
        university,
        department,
        otherUniversity,
        otherDepartment,
        request.educationLevel(),
        request.studyYear(),
        request.gpa(),
        clock.instant());
    StudentProfile saved = profiles.saveAndFlush(profile);
    if (adminActorId != null)
      audit.recordProfileCorrection(adminActorId, saved.getId(), before, auditSnapshot(saved), ip);
    return response(saved);
  }

  @Transactional(readOnly = true)
  public List<UniversityResponse> universities() {
    return universities.findByActiveTrueOrderByNameAsc().stream()
        .map(UniversityResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DepartmentResponse> departments(UUID universityId) {
    return departments.findByUniversityIdAndActiveTrueOrderByNameAsc(universityId).stream()
        .map(DepartmentResponse::from)
        .toList();
  }

  private University selectedUniversity(ProfileUpdateRequest request) {
    if (request.universityId() == null) return null;
    University value =
        universities
            .findById(request.universityId())
            .orElseThrow(
                () ->
                    error(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_UNIVERSITY",
                        "Üniversite seçimi geçersiz."));
    if (!value.isActive())
      throw error(HttpStatus.BAD_REQUEST, "INVALID_UNIVERSITY", "Üniversite aktif değil.");
    return value;
  }

  private Department selectedDepartment(ProfileUpdateRequest request, University university) {
    if (request.departmentId() == null) return null;
    Department value =
        departments
            .findById(request.departmentId())
            .orElseThrow(
                () ->
                    error(HttpStatus.BAD_REQUEST, "INVALID_DEPARTMENT", "Bölüm seçimi geçersiz."));
    if (!value.isActive()
        || university == null
        || !value.getUniversity().getId().equals(university.getId()))
      throw error(
          HttpStatus.BAD_REQUEST,
          "DEPARTMENT_UNIVERSITY_MISMATCH",
          "Bölüm seçilen üniversiteye ait değil.");
    return value;
  }

  private void validateFallbacks(
      UUID universityId, UUID departmentId, String otherUniversity, String otherDepartment) {
    if (universityId != null && otherUniversity != null)
      throw error(
          HttpStatus.BAD_REQUEST,
          "UNIVERSITY_CHOICE_CONFLICT",
          "Listeden üniversite veya diğer üniversite alanından yalnız birini kullanın.");
    if (departmentId != null && otherDepartment != null)
      throw error(
          HttpStatus.BAD_REQUEST,
          "DEPARTMENT_CHOICE_CONFLICT",
          "Listeden bölüm veya diğer bölüm alanından yalnız birini kullanın.");
  }

  private ProfileResponse response(StudentProfile p) {
    University u = p.getUniversity();
    Department d = p.getDepartment();
    return new ProfileResponse(
        p.getId(),
        p.getUser().getId(),
        p.getVersion(),
        encryption.decrypt(p.getNationalIdEncrypted()),
        p.getBirthDate(),
        p.getPhone(),
        p.getAddressLine(),
        p.getCity(),
        p.getPostalCode(),
        p.getCountryCode(),
        u == null ? null : u.getId(),
        u == null ? null : u.getName(),
        d == null ? null : d.getId(),
        d == null ? null : d.getName(),
        p.getOtherUniversity(),
        p.getOtherDepartment(),
        p.getEducationLevel(),
        p.getStudyYear(),
        p.getGpa(),
        p.getUpdatedAt());
  }

  private ProfileResponse empty(UUID userId) {
    return new ProfileResponse(
        null, userId, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null);
  }

  private String auditSnapshot(StudentProfile p) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("city", p.getCity());
    values.put("countryCode", p.getCountryCode());
    values.put("universityId", p.getUniversity() == null ? null : p.getUniversity().getId());
    values.put("departmentId", p.getDepartment() == null ? null : p.getDepartment().getId());
    values.put("otherUniversity", p.getOtherUniversity());
    values.put("otherDepartment", p.getOtherDepartment());
    values.put("educationLevel", p.getEducationLevel());
    values.put("studyYear", p.getStudyYear());
    values.put("gpa", p.getGpa());
    return objectMapper.writeValueAsString(values);
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String upper(String value) {
    String clean = clean(value);
    return clean == null ? null : clean.toUpperCase(Locale.ROOT);
  }

  private ProfileException versionConflict() {
    return error(
        HttpStatus.CONFLICT,
        "PROFILE_VERSION_CONFLICT",
        "Profil başka bir işlem tarafından güncellendi. Sayfayı yenileyin.");
  }

  private ProfileException error(HttpStatus status, String code, String message) {
    return new ProfileException(status, code, message);
  }
}
