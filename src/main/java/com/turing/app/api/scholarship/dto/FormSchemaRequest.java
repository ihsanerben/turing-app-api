package com.turing.app.api.scholarship.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record FormSchemaRequest(
    @NotNull Long version,
    @NotBlank @Size(max = 200) String name,
    @NotEmpty List<@Valid FormSectionRequest> sections) {}
