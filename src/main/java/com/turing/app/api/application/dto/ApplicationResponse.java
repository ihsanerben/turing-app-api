package com.turing.app.api.application.dto;

import com.turing.app.api.application.entity.*;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
    UUID id,
    UUID periodId,
    String periodName,
    String programName,
    UUID formId,
    int formVersion,
    ApplicationStatus status,
    int completion,
    Instant submittedAt,
    Instant createdAt,
    long version) {
  public static ApplicationResponse from(Application value) {
    return new ApplicationResponse(
        value.getId(),
        value.getPeriod().getId(),
        value.getPeriod().getName(),
        value.getPeriod().getProgram().getName(),
        value.getForm().getId(),
        value.getForm().getVersionNumber(),
        value.getStatus(),
        value.getCompletion(),
        value.getSubmittedAt(),
        value.getCreatedAt(),
        value.getVersion());
  }
}
