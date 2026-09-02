package com.turing.app.api.document.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record DocumentRequirementRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 1000) String description,
    boolean required,
    @NotEmpty List<@Pattern(regexp = "application/pdf|image/png|image/jpeg") String> allowedMimeTypes,
    @Min(1) @Max(10485760) long maxSizeBytes,
    @PositiveOrZero int order) {}
