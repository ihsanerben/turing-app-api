package com.turing.app.api.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AnswerRequest(@NotNull UUID fieldId, @NotNull Object value) {}
