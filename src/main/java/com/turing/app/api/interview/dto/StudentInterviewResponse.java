package com.turing.app.api.interview.dto;

import com.turing.app.api.interview.entity.*;
import java.time.Instant;
import java.util.UUID;

public record StudentInterviewResponse(
    UUID id,
    UUID applicationId,
    String programName,
    String periodName,
    Instant startsAt,
    Instant endsAt,
    InterviewStatus status,
    InterviewLocationType locationType,
    String location,
    String meetingUrl,
    long version) {
  public static StudentInterviewResponse from(Interview v) {
    return new StudentInterviewResponse(
        v.getId(),
        v.getApplication().getId(),
        v.getApplication().getPeriod().getProgram().getName(),
        v.getApplication().getPeriod().getName(),
        v.getStartsAt(),
        v.getEndsAt(),
        v.getStatus(),
        v.getLocationType(),
        v.getLocation(),
        v.getMeetingUrl(),
        v.getVersion());
  }
}
