package com.turing.app.api.auth.dto;

import jakarta.validation.constraints.*;

public record EmailRequest(@NotBlank @Email @Size(max = 320) String email) {}
