package com.turing.app.api.interview.dto;

import com.turing.app.api.interview.entity.InterviewLocationType;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record InterviewRequest(
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @NotNull InterviewLocationType locationType,
    @Size(max = 300) String location,
    @Size(max = 1000) String meetingUrl,
    Long version) {}
