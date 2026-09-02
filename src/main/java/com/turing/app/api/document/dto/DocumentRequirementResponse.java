package com.turing.app.api.document.dto;

import com.turing.app.api.document.entity.DocumentRequirement;
import java.util.*;

public record DocumentRequirementResponse(
    UUID id,
    UUID periodId,
    String name,
    String description,
    boolean required,
    List<String> allowedMimeTypes,
    long maxSizeBytes,
    int order) {
  public static DocumentRequirementResponse from(DocumentRequirement value) {
    return new DocumentRequirementResponse(
        value.getId(),
        value.getPeriod().getId(),
        value.getName(),
        value.getDescription(),
        value.isRequired(),
        value.getAllowedMimeTypes(),
        value.getMaxSizeBytes(),
        value.getDisplayOrder());
  }
}
