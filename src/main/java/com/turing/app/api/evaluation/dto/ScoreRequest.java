package com.turing.app.api.evaluation.dto;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record ScoreRequest(@NotNull @DecimalMin("0.00") @Digits(integer=6,fraction=2) BigDecimal score,@Size(max=2000) String comment,Long version){}
