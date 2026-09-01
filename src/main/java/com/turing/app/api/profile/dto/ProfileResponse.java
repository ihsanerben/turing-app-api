package com.turing.app.api.profile.dto;

import com.turing.app.api.profile.entity.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record ProfileResponse(
        UUID id, UUID userId, Long version, String nationalId, LocalDate birthDate, String phone,
        String addressLine, String city, String postalCode, String countryCode,
        UUID universityId, String universityName, UUID departmentId, String departmentName,
        String otherUniversity, String otherDepartment, EducationLevel educationLevel,
        Integer studyYear, BigDecimal gpa, Instant updatedAt) {}
