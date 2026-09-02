package com.turing.app.api.scholarship.dto;

import jakarta.validation.constraints.*;

public record ProgramRequest(
    @PositiveOrZero Long version,
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") @Size(max = 200) String slug,
    @NotBlank @Size(max = 10000) String description) {}
