package com.turing.app.api.participation.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MealDayRequest(@NotNull LocalDate date) {}
