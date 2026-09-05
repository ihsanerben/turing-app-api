package com.turing.app.api.participation.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record EventSelection(@NotNull UUID eventId, @NotNull Boolean attending) {}
