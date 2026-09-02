package com.turing.app.api.scholarship.dto;

import com.turing.app.api.scholarship.entity.FormFieldType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record FormFieldRequest(
    @NotBlank @Pattern(regexp="[a-z][a-z0-9_]*") @Size(max=80) String key,
    @NotBlank @Size(max=250) String label,
    @NotNull FormFieldType type,
    boolean required,
    @Size(max=250) String placeholder,
    UUID requirementId,
    @NotNull Map<String,Object> validationRules,
    @NotNull List<@Valid FormOptionRequest> options
) {}
