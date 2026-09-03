package com.turing.app.api.config.dto;

import com.turing.app.api.config.entity.AppConfig;
import org.springframework.stereotype.Component;

@Component
public class AppConfigMapper {
  public PublicAppConfigResponse toPublicResponse(AppConfig value) {
    return new PublicAppConfigResponse(
        value.getApplicationName(),
        value.getTagline(),
        value.getLogoUrl(),
        value.getPrimaryColor(),
        value.getSupportEmail(),
        value.getSupportPhone(),
        value.getContactAddress(),
        value.getFooterText(),
        value.isMaintenanceNoticeEnabled(),
        value.getMaintenanceNotice());
  }

  public AdminAppConfigResponse toAdminResponse(AppConfig value) {
    return new AdminAppConfigResponse(
        value.getApplicationName(),
        value.getTagline(),
        value.getLogoUrl(),
        value.getPrimaryColor(),
        value.getSupportEmail(),
        value.getSupportPhone(),
        value.getContactAddress(),
        value.getFooterText(),
        value.isMaintenanceNoticeEnabled(),
        value.getMaintenanceNotice(),
        value.getUpdatedAt(),
        value.getVersion());
  }
}
