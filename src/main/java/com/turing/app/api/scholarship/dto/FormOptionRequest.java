package com.turing.app.api.scholarship.dto;

import jakarta.validation.constraints.*;

public record FormOptionRequest(
    @NotBlank @Size(max = 200) String label,
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]+") @Size(max = 100) String value) {}
