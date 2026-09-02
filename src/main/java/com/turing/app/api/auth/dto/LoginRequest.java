package com.turing.app.api.auth.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
    @NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(max = 128) String password) {}
