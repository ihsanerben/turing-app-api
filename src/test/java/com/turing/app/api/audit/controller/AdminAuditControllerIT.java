package com.turing.app.api.audit.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AdminAuditControllerIT {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private UUID actorId;
  private UUID entityId;

  @BeforeEach
  void createAuditLog() {
    actorId = UUID.randomUUID();
    entityId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    jdbc.update(
        """
        INSERT INTO users (
            id, email, password_hash, first_name, last_name, role, account_status,
            email_verified_at, created_at, updated_at, version
        ) VALUES (?, ?, ?, ?, ?, 'ADMIN', 'ACTIVE', ?, ?, ?, 0)
        """,
        actorId,
        "audit-admin-" + actorId + "@example.com",
        "not-used",
        "Audit",
        "Admin",
        now,
        now,
        now);
    jdbc.update(
        """
        INSERT INTO audit_logs (
            id, actor_id, action, entity_type, entity_id, old_values, new_values,
            ip_reference, request_id, created_at
        ) VALUES (?, ?, 'APPLICATION_STATUS_CHANGED', 'APPLICATION', ?, '{}'::jsonb,
                  '{"status":"UNDER_REVIEW"}'::jsonb, '127.0.0.1', 'request-1', ?)
        """,
        UUID.randomUUID(),
        actorId,
        entityId,
        now);
  }

  @Test
  void requiresAdminAndReturnsFilteredPagedAuditEntries() throws Exception {
    mvc.perform(get("/api/admin/audit-logs")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/admin/audit-logs").with(user("student").roles("USER")))
        .andExpect(status().isForbidden());

    mvc.perform(
            get("/api/admin/audit-logs")
                .with(user("admin").roles("ADMIN"))
                .param("entityType", "application")
                .param("entityId", entityId.toString())
                .param("page", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].actorId").value(actorId.toString()))
        .andExpect(jsonPath("$.content[0].actorName").value("Audit Admin"))
        .andExpect(jsonPath("$.content[0].action").value("APPLICATION_STATUS_CHANGED"))
        .andExpect(jsonPath("$.content[0].newValues.status").value("UNDER_REVIEW"));
  }

  @Test
  void validatesPaginationAndSortDirection() throws Exception {
    mvc.perform(
            get("/api/admin/audit-logs").with(user("admin").roles("ADMIN")).param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PAGE"));

    mvc.perform(
            get("/api/admin/audit-logs")
                .with(user("admin").roles("ADMIN"))
                .param("direction", "sideways"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_SORT_DIRECTION"));
  }
}
