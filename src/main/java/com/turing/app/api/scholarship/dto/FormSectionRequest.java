package com.turing.app.api.scholarship.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record FormSectionRequest(@NotBlank @Size(max=200) String title,@Size(max=1000) String description,@NotEmpty List<@Valid FormFieldRequest> fields) {}
