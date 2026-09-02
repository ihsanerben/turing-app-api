package com.turing.app.api.scholarship.dto;
import com.turing.app.api.scholarship.entity.PeriodStatus;
import jakarta.validation.constraints.*;
public record PeriodStatusRequest(@NotNull @PositiveOrZero Long version,@NotNull PeriodStatus status){}
