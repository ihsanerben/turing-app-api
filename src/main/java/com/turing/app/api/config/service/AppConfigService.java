package com.turing.app.api.config.service;

import com.turing.app.api.audit.service.AuditService;
import com.turing.app.api.config.dto.AdminAppConfigResponse;
import com.turing.app.api.config.dto.AppConfigMapper;
import com.turing.app.api.config.dto.AppConfigUpdateRequest;
import com.turing.app.api.config.dto.PublicAppConfigResponse;
import com.turing.app.api.config.entity.AppConfig;
import com.turing.app.api.config.repository.AppConfigRepository;
import com.turing.app.api.content.exception.ContentException;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class AppConfigService {
  private final AppConfigRepository repository;
  private final AppConfigMapper mapper;
  private final AuditService audit;
  private final ObjectMapper json;
  private final Clock clock;

  public AppConfigService(
      AppConfigRepository repository,
      AppConfigMapper mapper,
      AuditService audit,
      ObjectMapper json,
      Clock clock) {
    this.repository = repository;
    this.mapper = mapper;
    this.audit = audit;
    this.json = json;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PublicAppConfigResponse publicConfig() {
    return mapper.toPublicResponse(config());
  }

  @Transactional(readOnly = true)
  public AdminAppConfigResponse adminConfig() {
    return mapper.toAdminResponse(config());
  }

  @Transactional
  public AdminAppConfigResponse update(UUID actorId, AppConfigUpdateRequest request, String ip) {
    AppConfig value = config();
    if (value.getVersion() != request.version()) {
      throw new ContentException(
          HttpStatus.CONFLICT,
          "VERSION_CONFLICT",
          "Ayarlar başka bir işlem tarafından güncellendi. Sayfayı yenileyin.");
    }
    if (request.maintenanceNoticeEnabled() && blank(request.maintenanceNotice()) == null) {
      throw new ContentException(
          HttpStatus.BAD_REQUEST,
          "MAINTENANCE_NOTICE_REQUIRED",
          "Bakım duyurusu etkinleştirildiğinde duyuru metni zorunludur.");
    }
    String oldValues = snapshot(value);
    value.update(
        request.applicationName().trim(),
        request.tagline().trim(),
        blank(request.logoUrl()),
        request.primaryColor().toUpperCase(),
        request.supportEmail().trim().toLowerCase(),
        blank(request.supportPhone()),
        blank(request.contactAddress()),
        request.footerText().trim(),
        request.maintenanceNoticeEnabled(),
        blank(request.maintenanceNotice()),
        clock.instant());
    repository.flush();
    audit.record(
        actorId, "APP_CONFIG_UPDATED", "APP_CONFIG", AppConfig.ID, oldValues, snapshot(value), ip);
    return mapper.toAdminResponse(value);
  }

  private AppConfig config() {
    return repository
        .findById(AppConfig.ID)
        .orElseThrow(
            () ->
                new ContentException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "APP_CONFIG_MISSING",
                    "Uygulama ayarları bulunamadı."));
  }

  private String blank(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String snapshot(AppConfig value) {
    return json.writeValueAsString(
        Map.of(
            "applicationName", value.getApplicationName(),
            "primaryColor", value.getPrimaryColor(),
            "supportEmail", value.getSupportEmail(),
            "maintenanceNoticeEnabled", value.isMaintenanceNoticeEnabled(),
            "version", value.getVersion()));
  }
}
