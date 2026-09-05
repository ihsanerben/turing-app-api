package com.turing.app.api.participation.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record EventRequest(
    @NotBlank @Size(max = 200) String title,
    @NotNull @Size(max = 3000) String description,
    @NotNull Instant startsAt,
    @NotNull @Size(max = 500) String location) {}
