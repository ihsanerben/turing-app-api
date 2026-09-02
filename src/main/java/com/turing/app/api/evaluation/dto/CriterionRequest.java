package com.turing.app.api.evaluation.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CriterionRequest(
    @NotBlank @Size(max = 160) String name,
    @Size(max = 1000) String description,
    @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal maxScore,
    @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal weight,
    @PositiveOrZero int displayOrder,
    Long version) {}
