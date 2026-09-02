package com.turing.app.api.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.turing.app.api.auth.service.AuthMailService;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import(AuthControllerIT.TestMailConfig.class)
class AuthControllerIT {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired MockMvc mockMvc;
  @Autowired CapturingMailService mail;

  @Test
  void completesRegistrationLoginRefreshAndDetectsReuse() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"user@example.com\",\"password\":\"strong-pass-123\",\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
        .andExpect(status().isCreated());
    assertThat(mail.verification.get()).isNotBlank();

    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + mail.verification.get() + "\"}"))
        .andExpect(status().isOk());

    MvcResult login =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"user@example.com\",\"password\":\"strong-pass-123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("USER"))
            .andReturn();
    Cookie access = login.getResponse().getCookie("TURING_ACCESS_TOKEN");
    Cookie firstRefresh = login.getResponse().getCookie("TURING_REFRESH_TOKEN");
    assertThat(access).isNotNull();
    assertThat(access.isHttpOnly()).isTrue();

    mockMvc
        .perform(get("/api/me").cookie(access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("user@example.com"));
    mockMvc.perform(get("/api/admin/ping").cookie(access)).andExpect(status().isForbidden());

    MvcResult refresh =
        mockMvc
            .perform(post("/api/auth/refresh").with(csrf()).cookie(firstRefresh))
            .andExpect(status().isOk())
            .andReturn();
    Cookie secondRefresh = refresh.getResponse().getCookie("TURING_REFRESH_TOKEN");
    mockMvc
        .perform(post("/api/auth/refresh").with(csrf()).cookie(firstRefresh))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));
    mockMvc
        .perform(post("/api/auth/refresh").with(csrf()).cookie(secondRefresh))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsStateChangingRequestWithoutCsrfToken() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"csrf@example.com\",\"password\":\"strong-pass-123\",\"firstName\":\"A\",\"lastName\":\"B\"}"))
        .andExpect(status().isForbidden());
  }

  @TestConfiguration
  static class TestMailConfig {
    @Bean
    @Primary
    CapturingMailService capturingMailService() {
      return new CapturingMailService();
    }
  }

  static class CapturingMailService implements AuthMailService {
    final AtomicReference<String> verification = new AtomicReference<>();
    final AtomicReference<String> reset = new AtomicReference<>();

    public void sendVerification(String email, String token) {
      verification.set(token);
    }

    public void sendPasswordReset(String email, String token) {
      reset.set(token);
    }
  }
}
