package com.turing.app.api.hardening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
class HardeningIT {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Test
  void enforcesThePublicAuthenticatedAndAdminRouteMatrix() throws Exception {
    mvc.perform(get("/api/health")).andExpect(status().isOk());
    mvc.perform(get("/api/public/faq")).andExpect(status().isNotFound());
    mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/admin/audit-logs")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/admin/audit-logs").with(user("student").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void addsBrowserSecurityHeaders() throws Exception {
    mvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Content-Security-Policy",
                    "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(
            header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"));
  }

  @Test
  void criticalListQueriesUseTheirDedicatedIndexes() {
    jdbc.execute("SET enable_seqscan = off");

    assertPlanUses(
        "EXPLAIN SELECT * FROM applications ORDER BY created_at DESC, id LIMIT 20",
        "idx_applications_admin_created");
    assertPlanUses(
        """
        EXPLAIN SELECT * FROM applications
        WHERE period_id = '00000000-0000-0000-0000-000000000001'
          AND status = 'SUBMITTED'
        ORDER BY created_at DESC, id
        LIMIT 20
        """,
        "idx_applications_period_status_created");
    assertPlanUses(
        """
        EXPLAIN SELECT * FROM audit_logs
        WHERE action = 'APPLICATION_STATUS_CHANGED'
        ORDER BY created_at DESC, id
        LIMIT 20
        """,
        "idx_audit_action_created");
  }

  private void assertPlanUses(String sql, String indexName) {
    List<String> plan = jdbc.query(sql, (result, row) -> result.getString(1));
    assertThat(String.join("\n", plan)).contains(indexName);
  }
}
