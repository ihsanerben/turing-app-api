package com.turing.app.api.participation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record EventSelectionRequest(
    @NotEmpty @Size(max = 100) List<@NotNull @Valid EventSelection> changes,
    @NotNull @PositiveOrZero Long version) {}
