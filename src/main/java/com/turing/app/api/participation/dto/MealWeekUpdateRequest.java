package com.turing.app.api.participation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record MealWeekUpdateRequest(
    @NotEmpty @Size(max = 7) List<@NotNull @Valid MealDayRequest> days,
    @NotNull @PositiveOrZero Long version) {}
