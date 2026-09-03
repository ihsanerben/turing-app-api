package com.turing.app.api.user.service;

import com.turing.app.api.profile.entity.StudentProfile;
import com.turing.app.api.profile.repository.StudentProfileRepository;
import com.turing.app.api.profile.security.NationalIdEncryptionService;
import com.turing.app.api.user.dto.AdminUserResponse;
import com.turing.app.api.user.entity.Role;
import com.turing.app.api.user.entity.User;
import com.turing.app.api.user.exception.UserManagementException;
import com.turing.app.api.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
  private final UserRepository users;
  private final StudentProfileRepository profiles;
  private final NationalIdEncryptionService encryption;

  public AdminUserService(
      UserRepository users,
      StudentProfileRepository profiles,
      NationalIdEncryptionService encryption) {
    this.users = users;
    this.profiles = profiles;
    this.encryption = encryption;
  }

  @Transactional(readOnly = true)
  public List<AdminUserResponse> list(Role role) {
    return users.findByRoleOrderByCreatedAtDesc(role).stream().map(this::response).toList();
  }

  @Transactional(readOnly = true)
  public AdminUserResponse get(UUID id) {
    User user =
        users
            .findById(id)
            .orElseThrow(
                () ->
                    new UserManagementException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Kullanıcı bulunamadı."));
    return response(user);
  }

  private AdminUserResponse response(User user) {
    StudentProfile profile = profiles.findByUserId(user.getId()).orElse(null);
    return new AdminUserResponse(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getRole(),
        user.getAccountStatus(),
        user.getEmailVerifiedAt(),
        user.getLastLoginAt(),
        user.getCreatedAt(),
        nationalId(profile),
        profile == null ? null : profile.getBirthDate(),
        profile == null ? null : profile.getPhone(),
        profile == null ? null : profile.getAddressLine(),
        profile == null ? null : profile.getCity(),
        profile == null ? null : profile.getPostalCode(),
        profile == null ? null : profile.getCountryCode(),
        profile == null || profile.getUniversity() == null
            ? null
            : profile.getUniversity().getName(),
        profile == null || profile.getDepartment() == null
            ? null
            : profile.getDepartment().getName(),
        profile == null ? null : profile.getOtherUniversity(),
        profile == null ? null : profile.getOtherDepartment(),
        profile == null ? null : profile.getEducationLevel(),
        profile == null ? null : profile.getStudyYear(),
        profile == null ? null : profile.getGpa());
  }

  private String nationalId(StudentProfile profile) {
    if (profile == null) return null;
    if (profile.getNationalId() != null) return profile.getNationalId();
    return encryption.decrypt(profile.getNationalIdEncrypted());
  }
}
