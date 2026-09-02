package com.turing.app.api.scholarship.dto;

import jakarta.validation.constraints.*;

public record VersionRequest(@NotNull @PositiveOrZero Long version) {}
