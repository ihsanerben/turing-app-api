package com.turing.app.api.scholarship.dto;

import jakarta.validation.constraints.*;

public record FormCreateRequest(@NotBlank @Size(max = 200) String name) {}
