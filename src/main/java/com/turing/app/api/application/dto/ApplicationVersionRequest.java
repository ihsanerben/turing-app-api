package com.turing.app.api.application.dto;

import jakarta.validation.constraints.NotNull;

public record ApplicationVersionRequest(@NotNull Long version) {}
