package com.turing.app.api.interview.dto;

import com.turing.app.api.interview.entity.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public record AdminInterviewResponse(
    UUID id,
    UUID applicationId,
    String studentName,
    String programName,
    Instant startsAt,
    Instant endsAt,
    InterviewStatus status,
    InterviewLocationType locationType,
    String location,
    String meetingUrl,
    String createdBy,
    long version,
    List<Feedback> feedback) {
  public record Feedback(
      UUID id,
      UUID interviewerId,
      String interviewerName,
      BigDecimal score,
      String notes,
      String recommendation,
      long version) {}
}
