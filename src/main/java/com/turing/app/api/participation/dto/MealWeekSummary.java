package com.turing.app.api.participation.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MealWeekSummary(UUID id, LocalDate weekStart) {}
