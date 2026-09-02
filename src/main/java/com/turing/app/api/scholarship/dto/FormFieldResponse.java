package com.turing.app.api.scholarship.dto;

import com.turing.app.api.scholarship.entity.*;
import java.util.*;

public record FormFieldResponse(
    UUID id,
    String key,
    String label,
    FormFieldType type,
    boolean required,
    int order,
    String placeholder,
    UUID requirementId,
    Map<String, Object> validationRules,
    List<FormOptionResponse> options) {
  public static FormFieldResponse from(FormField value) {
    return new FormFieldResponse(
        value.getId(),
        value.getKey(),
        value.getLabel(),
        value.getType(),
        value.isRequired(),
        value.getDisplayOrder(),
        value.getPlaceholder(),
        value.getRequirementId(),
        value.getValidationRules(),
        value.getOptions().stream().map(FormOptionResponse::from).toList());
  }
}
