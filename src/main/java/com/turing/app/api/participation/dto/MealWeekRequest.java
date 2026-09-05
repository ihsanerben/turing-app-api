package com.turing.app.api.participation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record MealWeekRequest(
    @NotNull LocalDate weekStart,
    @NotEmpty @Size(max = 7) List<@NotNull @Valid MealDayRequest> days) {}
