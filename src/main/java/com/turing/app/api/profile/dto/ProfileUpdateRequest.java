package com.turing.app.api.profile.dto;

import com.turing.app.api.profile.entity.EducationLevel;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProfileUpdateRequest(
    @PositiveOrZero Long version,
    @Pattern(
            regexp = "^[1-9][0-9]{10}$",
            message = "Kimlik numarası 11 rakam olmalı ve sıfırla başlamamalıdır.")
        String nationalId,
    @Past LocalDate birthDate,
    @Pattern(regexp = "^\\+?[0-9 ()-]{7,32}$") String phone,
    @Size(max = 300) String addressLine,
    @Size(max = 100) String city,
    @Size(max = 20) String postalCode,
    @Pattern(regexp = "^[A-Za-z]{2}$") String countryCode,
    UUID universityId,
    UUID departmentId,
    @Size(max = 200) String otherUniversity,
    @Size(max = 200) String otherDepartment,
    EducationLevel educationLevel,
    @Min(1) @Max(8) Integer studyYear,
    @DecimalMin("0.00") @DecimalMax("4.00") BigDecimal gpa) {}
