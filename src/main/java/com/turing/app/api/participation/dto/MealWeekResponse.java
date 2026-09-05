package com.turing.app.api.participation.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MealWeekResponse(
    UUID id,
    LocalDate weekStart,
    List<ActivityResponse> days,
    long version,
    long scheduleVersion) {}
