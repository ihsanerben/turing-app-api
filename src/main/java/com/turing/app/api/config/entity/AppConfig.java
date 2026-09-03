package com.turing.app.api.config.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_config")
public class AppConfig {
  public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Id private UUID id;

  @Column(name = "application_name", nullable = false, length = 100)
  private String applicationName;

  @Column(nullable = false, length = 240)
  private String tagline;

  @Column(name = "logo_url", length = 500)
  private String logoUrl;

  @Column(name = "primary_color", nullable = false, length = 7)
  private String primaryColor;

  @Column(name = "support_email", nullable = false, length = 320)
  private String supportEmail;

  @Column(name = "support_phone", length = 40)
  private String supportPhone;

  @Column(name = "contact_address", length = 500)
  private String contactAddress;

  @Column(name = "footer_text", nullable = false, length = 300)
  private String footerText;

  @Column(name = "maintenance_notice_enabled", nullable = false)
  private boolean maintenanceNoticeEnabled;

  @Column(name = "maintenance_notice", length = 500)
  private String maintenanceNotice;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected AppConfig() {}

  public void update(
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
      Instant now) {
    this.applicationName = applicationName;
    this.tagline = tagline;
    this.logoUrl = logoUrl;
    this.primaryColor = primaryColor;
    this.supportEmail = supportEmail;
    this.supportPhone = supportPhone;
    this.contactAddress = contactAddress;
    this.footerText = footerText;
    this.maintenanceNoticeEnabled = maintenanceNoticeEnabled;
    this.maintenanceNotice = maintenanceNotice;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public String getTagline() {
    return tagline;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public String getPrimaryColor() {
    return primaryColor;
  }

  public String getSupportEmail() {
    return supportEmail;
  }

  public String getSupportPhone() {
    return supportPhone;
  }

  public String getContactAddress() {
    return contactAddress;
  }

  public String getFooterText() {
    return footerText;
  }

  public boolean isMaintenanceNoticeEnabled() {
    return maintenanceNoticeEnabled;
  }

  public String getMaintenanceNotice() {
    return maintenanceNotice;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }
}
