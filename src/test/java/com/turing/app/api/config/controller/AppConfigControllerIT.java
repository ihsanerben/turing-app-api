package com.turing.app.api.config.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.turing.app.api.auth.security.AuthenticatedUser;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AppConfigControllerIT {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private UUID adminId;

  @BeforeEach
  void createAdmin() {
    adminId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    jdbc.update(
        """
        INSERT INTO users (
            id, email, password_hash, first_name, last_name, role, account_status,
            email_verified_at, created_at, updated_at, version
        ) VALUES (?, ?, 'not-used', 'Config', 'Admin', 'ADMIN', 'ACTIVE', ?, ?, ?, 0)
        """,
        adminId,
        "config-admin-" + adminId + "@example.com",
        now,
        now,
        now);
  }

  @Test
  void exposesOnlyPublicFieldsAndProtectsAdminView() throws Exception {
    mvc.perform(get("/api/public/app-config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applicationName").value("Turing Otomobil Kurumu"))
        .andExpect(jsonPath("$.supportEmail").value("info@turing.local"))
        .andExpect(jsonPath("$.version").doesNotExist())
        .andExpect(jsonPath("$.updatedAt").doesNotExist());

    mvc.perform(get("/api/admin/app-config")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/admin/app-config").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(0));
  }

  @Test
  void validatesUpdatesUsesOptimisticVersionAndWritesAudit() throws Exception {
    String body =
        """
        {
          "applicationName": "Turing Bursları",
          "tagline": "Eğitim yolculuğunda yanınızda.",
          "logoUrl": "https://example.com/logo.png",
          "primaryColor": "#123ABC",
          "supportEmail": "destek@example.com",
          "supportPhone": "+90 212 000 00 00",
          "contactAddress": "İstanbul",
          "footerText": "Turing Bursları",
          "maintenanceNoticeEnabled": true,
          "maintenanceNotice": "Planlı bakım cumartesi günü yapılacaktır.",
          "version": 0
        }
        """;

    mvc.perform(
            put("/api/admin/app-config")
                .with(admin())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applicationName").value("Turing Bursları"))
        .andExpect(jsonPath("$.version").value(1));

    mvc.perform(
            put("/api/admin/app-config")
                .with(admin())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));

    Integer auditCount =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit_logs WHERE action = 'APP_CONFIG_UPDATED' AND actor_id = ?",
            Integer.class,
            adminId);
    org.assertj.core.api.Assertions.assertThat(auditCount).isEqualTo(1);
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
    AuthenticatedUser principal =
        new AuthenticatedUser(adminId, "config-admin@example.com", "ADMIN");
    return authentication(
        new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
  }
}
