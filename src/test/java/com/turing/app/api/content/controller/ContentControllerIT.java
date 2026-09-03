package com.turing.app.api.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.turing.app.api.auth.service.AuthMailService;
import com.turing.app.api.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import(ContentControllerIT.MailConfig.class)
class ContentControllerIT {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired org.springframework.test.web.servlet.MockMvc mvc;
  @Autowired Mail mail;
  @Autowired UserRepository users;
  @Autowired JdbcTemplate jdbc;

  @Test
  void hidesDraftAndArchivedContentAndPublishesOnlyActiveContent() throws Exception {
    Cookie admin = admin("content-admin@example.com");
    String announcement =
        JsonPath.read(
            mvc.perform(
                    post("/api/admin/announcements")
                        .with(csrf())
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"title\":\"Yeni dönem\",\"slug\":\"yeni-donem\",\"summary\":\"Başvurular açılıyor\",\"content\":\"Ayrıntılı duyuru metni\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");
    mvc.perform(get("/api/public/announcements"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    mvc.perform(get("/api/public/announcements/yeni-donem")).andExpect(status().isNotFound());
    mvc.perform(
            post("/api/admin/announcements/" + announcement + "/publish")
                .with(csrf())
                .cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"))
        .andExpect(jsonPath("$.publishedAt").isNotEmpty());
    mvc.perform(get("/api/public/announcements"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].slug").value("yeni-donem"));
    mvc.perform(
            post("/api/admin/announcements/" + announcement + "/archive")
                .with(csrf())
                .cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/public/announcements"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    mvc.perform(
            post("/api/admin/announcements/" + announcement + "/restore")
                .with(csrf())
                .cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DRAFT"));
    mvc.perform(
            put("/api/admin/announcements/" + announcement)
                .with(csrf())
                .cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Güncel dönem\",\"slug\":\"yeni-donem\",\"summary\":\"Güncel özet\",\"content\":\"Güncel içerik\",\"version\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Güncel dönem"));
    mvc.perform(
            post("/api/admin/announcements/" + announcement + "/publish")
                .with(csrf())
                .cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":4}"))
        .andExpect(status().isOk());
    mvc.perform(
            delete("/api/admin/announcements/" + announcement + "?version=5")
                .with(csrf())
                .cookie(admin))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/admin/announcements/" + announcement).cookie(admin))
        .andExpect(status().isNotFound());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_logs where entity_type='ANNOUNCEMENT'", Integer.class))
        .isEqualTo(7);
  }

  private Cookie admin(String email) throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\""
                        + email
                        + "\",\"password\":\"strong-pass-123\",\"firstName\":\"Test\",\"lastName\":\"Admin\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/auth/verify-email")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + mail.tokens.get(email) + "\"}"))
        .andExpect(status().isOk());
    UUID id = users.findByEmailIgnoreCase(email).orElseThrow().getId();
    jdbc.update("update users set role='ADMIN' where id=?", id);
    return mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"strong-pass-123\"}"))
        .andReturn()
        .getResponse()
        .getCookie("TURING_ACCESS_TOKEN");
  }

  @TestConfiguration
  static class MailConfig {
    @Bean
    @Primary
    Mail mail() {
      return new Mail();
    }
  }

  static class Mail implements AuthMailService {
    final Map<String, String> tokens = new ConcurrentHashMap<>();

    public void sendVerification(String email, String token) {
      tokens.put(email, token);
    }

    public void sendPasswordReset(String email, String token) {}
  }
}
