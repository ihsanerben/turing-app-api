package com.turing.app.api.config.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AppConfigUpdateRequest(
    @NotBlank @Size(max = 100) String applicationName,
    @NotBlank @Size(max = 240) String tagline,
    @Size(max = 500) @Pattern(regexp = "^$|https?://.+") String logoUrl,
    @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String primaryColor,
    @NotBlank @Email @Size(max = 320) String supportEmail,
    @Size(max = 40) String supportPhone,
    @Size(max = 500) String contactAddress,
    @NotBlank @Size(max = 300) String footerText,
    boolean maintenanceNoticeEnabled,
    @Size(max = 500) String maintenanceNotice,
    @NotNull @PositiveOrZero Long version) {}
