package com.turing.app.api.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApplicationCreateRequest(@NotNull UUID periodId) {}
