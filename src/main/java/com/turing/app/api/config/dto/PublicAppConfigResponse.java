package com.turing.app.api.config.dto;

public record PublicAppConfigResponse(
    String applicationName,
    String tagline,
    String logoUrl,
    String primaryColor,
    String supportEmail,
    String supportPhone,
    String contactAddress,
    String footerText,
    boolean maintenanceNoticeEnabled,
    String maintenanceNotice) {}
