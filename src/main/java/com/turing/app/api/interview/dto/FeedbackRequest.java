package com.turing.app.api.interview.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FeedbackRequest(
    @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal score,
    @NotBlank @Size(max = 4000) String notes,
    @Size(max = 2000) String recommendation,
    Long version) {}
