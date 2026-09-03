package com.turing.app.api.user.dto;

import com.turing.app.api.profile.entity.EducationLevel;
import com.turing.app.api.user.entity.AccountStatus;
import com.turing.app.api.user.entity.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminUserResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    Role role,
    AccountStatus accountStatus,
    Instant emailVerifiedAt,
    Instant lastLoginAt,
    Instant createdAt,
    String nationalId,
    LocalDate birthDate,
    String phone,
    String addressLine,
    String city,
    String postalCode,
    String countryCode,
    String universityName,
    String departmentName,
    String otherUniversity,
    String otherDepartment,
    EducationLevel educationLevel,
    Integer studyYear,
    BigDecimal gpa) {}
