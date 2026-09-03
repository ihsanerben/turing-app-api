package com.turing.app.api.scholarship.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record FormOptionRequest(
    UUID id,
    @NotBlank @Size(max = 200) String label,
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]+") @Size(max = 100) String value) {}
