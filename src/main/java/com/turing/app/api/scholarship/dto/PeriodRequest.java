package com.turing.app.api.scholarship.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record PeriodRequest(
    @PositiveOrZero Long version,
    @NotNull UUID programId,
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Pattern(regexp = "^[0-9]{4}-[0-9]{4}$") String academicYear,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @Positive Integer maxRecipients,
    boolean allowWithdrawal) {}
