package com.turing.app.api.auth.dto;

import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 512) String token,
        @NotBlank @Size(min = 10, max = 128) String password) {}
