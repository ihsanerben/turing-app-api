package com.turing.app.api.config.dto;

import java.time.Instant;

public record AdminAppConfigResponse(
    String applicationName,
    String tagline,
    String logoUrl,
    String primaryColor,
    String supportEmail,
    String supportPhone,
    String contactAddress,
    String footerText,
    boolean maintenanceNoticeEnabled,
    String maintenanceNotice,
    Instant updatedAt,
    long version) {}
