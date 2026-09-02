package com.turing.app.api.evaluation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.turing.app.api.auth.service.AuthMailService;
import com.turing.app.api.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
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
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import(EvaluationControllerIT.MailConfig.class)
class EvaluationControllerIT {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired MockMvc mvc;
  @Autowired Mail mail;
  @Autowired UserRepository users;
  @Autowired JdbcTemplate jdbc;

  @Test
  void evaluatesWithReviewerAverageWeightedTotalLimitsAndOptimisticConcurrency() throws Exception {
    Cookie admin = admin("evaluation-admin@example.com"),
        second = admin("evaluation-second@example.com"),
        student = session("evaluation-student@example.com");
    UUID studentId =
        users.findByEmailIgnoreCase("evaluation-student@example.com").orElseThrow().getId();
    UUID period = UUID.randomUUID(), application = seed(studentId, period);
    mvc.perform(
            get("/api/admin/application-periods/" + period + "/evaluation-criteria")
                .cookie(student))
        .andExpect(status().isForbidden());
    String first = criterion(admin, period, "Akademik başarı", 10, 60, 0);
    String secondCriterion = criterion(admin, period, "Maddi ihtiyaç", 20, 40, 1);
    mvc.perform(
            put("/api/admin/applications/" + application + "/evaluation-scores/" + first)
                .with(csrf())
                .cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":8,\"comment\":\"Başarılı\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weightedTotal").value(80.000));
    mvc.perform(
            put("/api/admin/applications/" + application + "/evaluation-scores/" + secondCriterion)
                .with(csrf())
                .cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weightedTotal").value(68.000));
    mvc.perform(
            put("/api/admin/applications/" + application + "/evaluation-scores/" + first)
                .with(csrf())
                .cookie(second)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":6}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weightedTotal").value(62.000))
        .andExpect(jsonPath("$.scores.length()").value(3));
    mvc.perform(
            put("/api/admin/applications/" + application + "/evaluation-scores/" + first)
                .with(csrf())
                .cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":11,\"version\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SCORE_EXCEEDS_MAX"));
    mvc.perform(
            put("/api/admin/applications/" + application + "/evaluation-scores/" + first)
                .with(csrf())
                .cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":9,\"version\":99}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EVALUATION_VERSION_CONFLICT"));
    mvc.perform(
            put("/api/admin/evaluation-criteria/" + first)
                .with(csrf())
                .cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Akademik başarı\",\"description\":\"\",\"maxScore\":20,\"weight\":60,\"displayOrder\":0,\"version\":0}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CRITERION_SCORING_STARTED"));
    mvc.perform(get("/api/admin/application-periods/" + period + "/ranking").cookie(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].rank").value(1))
        .andExpect(jsonPath("$[0].weightedTotal").value(62.000));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from evaluation_scores where application_id=?",
                Integer.class,
                application))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "select calculated_score from applications where id=?",
                java.math.BigDecimal.class,
                application))
        .isEqualByComparingTo("62.000");
  }

  private String criterion(Cookie admin, UUID period, String name, int max, int weight, int order)
      throws Exception {
    return JsonPath.read(
        mvc.perform(
                post("/api/admin/application-periods/" + period + "/evaluation-criteria")
                    .with(csrf())
                    .cookie(admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\""
                            + name
                            + "\",\"maxScore\":"
                            + max
                            + ",\"weight\":"
                            + weight
                            + ",\"displayOrder\":"
                            + order
                            + "}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$.id");
  }

  private UUID seed(UUID studentId, UUID period) {
    Instant instant = Instant.now();
    java.time.OffsetDateTime now =
        java.time.OffsetDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
    UUID profile = UUID.randomUUID(),
        program = UUID.randomUUID(),
        form = UUID.randomUUID(),
        application = UUID.randomUUID();
    jdbc.update(
        "insert into student_profiles(id,user_id,created_at,updated_at,version) values(?,?,?, ?,0)",
        profile,
        studentId,
        now,
        now);
    jdbc.update(
        "insert into scholarship_programs(id,name,slug,description,active,created_at,updated_at,version) values(?,?,?,?,true,?,?,0)",
        program,
        "Evaluation Program",
        "evaluation-" + program,
        "Test",
        now,
        now);
    jdbc.update(
        "insert into application_periods(id,program_id,name,academic_year,starts_at,ends_at,status,allow_withdrawal,created_at,updated_at,version) values(?,?,?,?,?,?,'EVALUATION',true,?,?,0)",
        period,
        program,
        "Evaluation Period",
        "2026-2027",
        now.minusHours(1),
        now.plusHours(1),
        now,
        now);
    jdbc.update(
        "insert into forms(id,period_id,name,version_number,status,published_at,created_at,updated_at,version) values(?,?,?,1,'PUBLISHED',?,?,?,0)",
        form,
        period,
        "Form",
        now,
        now,
        now);
    jdbc.update(
        "insert into applications(id,profile_id,period_id,form_id,status,completion,submitted_at,created_at,updated_at,version) values(?,?,?,?,'UNDER_REVIEW',100,?,?,?,0)",
        application,
        profile,
        period,
        form,
        now,
        now,
        now);
    return application;
  }

  private Cookie admin(String email) throws Exception {
    session(email);
    UUID id = users.findByEmailIgnoreCase(email).orElseThrow().getId();
    jdbc.update("update users set role='ADMIN' where id=?", id);
    return login(email);
  }

  private Cookie session(String email) throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\""
                        + email
                        + "\",\"password\":\"strong-pass-123\",\"firstName\":\"Test\",\"lastName\":\"User\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/auth/verify-email")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + mail.tokens.get(email) + "\"}"))
        .andExpect(status().isOk());
    return login(email);
  }

  private Cookie login(String email) throws Exception {
    return mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"strong-pass-123\"}"))
        .andExpect(status().isOk())
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
