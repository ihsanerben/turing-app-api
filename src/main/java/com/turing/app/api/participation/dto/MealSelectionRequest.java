package com.turing.app.api.participation.dto;

import jakarta.validation.constraints.*;
import java.util.Set;
import java.util.UUID;

public record MealSelectionRequest(
    @NotNull @Size(max = 7) Set<@NotNull UUID> dayIds, @NotNull @PositiveOrZero Long version) {}
