package com.turing.app.api.participation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.notification.mail.EmailSender;
import com.turing.app.api.notification.service.EmailCampaignService;
import com.turing.app.api.participation.dto.*;
import com.turing.app.api.participation.service.ParticipationService;
import com.turing.app.api.user.entity.User;
import com.turing.app.api.user.repository.UserRepository;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import(ParticipationControllerIT.Config.class)
class ParticipationControllerIT {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository users;
  @Autowired ParticipationService service;
  @Autowired EmailCampaignService campaigns;
  @Autowired JdbcTemplate jdbc;
  @Autowired Mail mail;
  @Autowired TransactionTemplate transactions;

  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean(name = "emailExecutor")
  Executor emailExecutor;

  @Test
  void mealSelectionIsPrivateAtomicIdempotentAndEmailsFullWeek() throws Exception {
    User admin = account(), student = account(), other = account();
    var week = week(admin);
    var first = week.days().getFirst();
    var second = week.days().getLast();
    String url = "/api/me/meal-weeks/" + week.id() + "/selection";
    var body = new MealSelectionRequest(Set.of(first.id(), second.id()), 0L);
    mvc.perform(
            put(url)
                .with(as(student, "USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1));
    awaitMail(student, 1);
    assertThat(mail.messages.get(student.getEmail()).getFirst())
        .contains(
            first.date().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            second.date().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    mvc.perform(
            put(url)
                .with(as(student, "USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changed").value(false));
    assertThat(campaignCount(student)).isEqualTo(1);
    mvc.perform(get("/api/me/meal-weeks/" + week.id()).with(as(other, "USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days[0].attending").value(false))
        .andExpect(jsonPath("$.days[0].participants").doesNotExist());
    mvc.perform(
            get("/api/admin/participation/" + first.id() + "/participants")
                .with(as(student, "USER")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/admin/participation/" + first.id() + "/participants")
                .with(as(admin, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].firstName").value(student.getFirstName()))
        .andExpect(jsonPath("$.content[0].email").doesNotExist());
    mvc.perform(
            put(url)
                .with(as(student, "USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new MealSelectionRequest(Set.of(first.id()), 0L))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SELECTION_VERSION_CONFLICT"));
    mvc.perform(
            put(url)
                .with(as(student, "USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        new MealSelectionRequest(Set.of(UUID.randomUUID()), 1L))))
        .andExpect(status().isBadRequest());
    assertThat(service.week(week.id(), student.getId()).days())
        .allMatch(ActivityResponse::attending);
    service.saveMeals(student.getId(), week.id(), new MealSelectionRequest(Set.of(), 1L));
    awaitMail(student, 2);
    assertThat(mail.messages.get(student.getEmail()).getLast())
        .contains("yemek kaydınız bulunmuyor");
    assertThat(service.participants(first.id(), 0, 50).totalElements()).isZero();
  }

  @Test
  void eventBatchPreservesOtherPagesAndRollsBackInvalidChanges() throws Exception {
    User admin = account(), student = account();
    var first = event(admin, "Gezi");
    var second = event(admin, "Konser");
    service.saveEvents(
        student.getId(),
        new EventSelectionRequest(List.of(new EventSelection(first.id(), true)), 0L));
    awaitMail(student, 1);
    service.saveEvents(
        student.getId(),
        new EventSelectionRequest(List.of(new EventSelection(second.id(), true)), 1L));
    awaitMail(student, 2);
    assertThat(service.participants(first.id(), 0, 10).totalElements()).isEqualTo(1);
    assertThat(mail.messages.get(student.getEmail()).getLast()).contains("Gezi", "Konser");
    mvc.perform(
            put("/api/me/events/selection")
                .with(as(student, "USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        new EventSelectionRequest(
                            List.of(
                                new EventSelection(first.id(), false),
                                new EventSelection(UUID.randomUUID(), true)),
                            2L))))
        .andExpect(status().isNotFound());
    assertThat(service.participants(first.id(), 0, 10).totalElements()).isEqualTo(1);
    assertThat(campaignCount(student)).isEqualTo(2);
    service.saveEvents(
        student.getId(),
        new EventSelectionRequest(List.of(new EventSelection(first.id(), false)), 2L));
    awaitMail(student, 3);
    assertThat(mail.messages.get(student.getEmail()).getLast())
        .contains("Katılım iptal edildi: Gezi", "Konser");
  }

  @Test
  void endpointsValidateRolesCsrfDatesAndPaginationAndExposeOpenApi() throws Exception {
    User student = account(), admin = account();
    mvc.perform(get("/api/me/events")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/admin/events")
                .with(as(student, "USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/admin/events")
                .with(as(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/admin/events")
                .with(as(admin, "ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/admin/events")
                .with(as(admin, "ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        new EventRequest("Gezi", "", Instant.now().minusSeconds(1), ""))))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/me/events?size=101").with(as(student, "USER")))
        .andExpect(status().isBadRequest());
    var week = week(admin);
    mvc.perform(
            post("/api/admin/meal-weeks")
                .with(as(admin, "ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        new MealWeekRequest(
                            week.weekStart(),
                            List.of(new MealDayRequest(week.days().getFirst().date()))))))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/me/events/selection")
                .with(as(student, "USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"changes\":[null],\"version\":0}"))
        .andExpect(status().isBadRequest());
    String openApi =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/me/events/selection'].put").exists())
            .andExpect(jsonPath("$.paths['/api/admin/meal-weeks'].post").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Files.writeString(Path.of("target/participation-openapi.json"), openApi);
  }

  @Test
  void parallelDifferentSelectionsConflictAndDuplicateRequestsSendOnce() throws Exception {
    User admin = account(), student = account();
    var first = event(admin, "Bir");
    var second = event(admin, "İki");
    CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<Integer>> futures = new ArrayList<>();
      for (var event : List.of(first, second))
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return mvc.perform(
                          put("/api/me/events/selection")
                              .with(as(student, "USER"))
                              .with(csrf())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(
                                  json.writeValueAsString(
                                      new EventSelectionRequest(
                                          List.of(new EventSelection(event.id(), true)), 0L))))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      assertThat(List.of(futures.get(0).get(), futures.get(1).get()))
          .containsExactlyInAnyOrder(200, 409);
    }
    assertThat(campaignCount(student)).isEqualTo(1);
    User duplicate = account();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var request = new EventSelectionRequest(List.of(new EventSelection(first.id(), true)), 0L);
      var a = executor.submit(() -> service.saveEvents(duplicate.getId(), request));
      var b = executor.submit(() -> service.saveEvents(duplicate.getId(), request));
      assertThat(List.of(a.get().changed(), b.get().changed()))
          .containsExactlyInAnyOrder(true, false);
    }
    assertThat(campaignCount(duplicate)).isEqualTo(1);
  }

  @Test
  void closedDaysCannotChangeAndRollbackDoesNotSendMail() throws Exception {
    User admin = account(), student = account();
    var week = week(admin);
    UUID day = week.days().getFirst().id();
    jdbc.update(
        "update participation_activities set meal_date=? where id=?",
        LocalDate.now(ZoneId.of("Europe/Istanbul")),
        day);
    mvc.perform(
            put("/api/me/meal-weeks/" + week.id() + "/selection")
                .with(as(student, "USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new MealSelectionRequest(Set.of(day), 0L))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("REGISTRATION_CLOSED"));
    var event = event(admin, "Rollback");
    transactions.executeWithoutResult(
        status -> {
          service.saveEvents(
              student.getId(),
              new EventSelectionRequest(List.of(new EventSelection(event.id(), true)), 0L));
          status.setRollbackOnly();
        });
    assertThat(campaignCount(student)).isZero();
    assertThat(service.participants(event.id(), 0, 10).totalElements()).isZero();
    assertThat(mail.messages).doesNotContainKey(student.getEmail());
  }

  @Test
  void smtpFailurePreservesRegistrationAndAllowsAdminRetry() throws Exception {
    User admin = account(), student = account();
    mail.fail.add(student.getEmail());
    var event = event(admin, "E-posta testi");
    service.saveEvents(
        student.getId(),
        new EventSelectionRequest(List.of(new EventSelection(event.id(), true)), 0L));
    UUID campaign =
        jdbc.queryForObject(
            "select id from email_campaigns where created_by=?", UUID.class, student.getId());
    for (int i = 0; i < 100 && !campaigns.detail(campaign).status().equals("COMPLETED"); i++)
      Thread.sleep(20);
    assertThat(campaigns.detail(campaign).recipients().getFirst().status()).isEqualTo("FAILED");
    assertThat(service.participants(event.id(), 0, 10).totalElements()).isEqualTo(1);
    mail.fail.remove(student.getEmail());
    campaigns.retry(admin.getId(), campaign, campaigns.detail(campaign).version(), null);
    awaitMail(student, 1);
  }

  @Test
  void rejectedEmailTaskDoesNotLoseSavedSelectionsAndCanBeRetried() throws Exception {
    User admin = account();
    User student = account();
    var event = event(admin, "Yoğunluk testi");
    doThrow(new RejectedExecutionException("Test capacity"))
        .when(emailExecutor)
        .execute(any(Runnable.class));
    var saved =
        service.saveEvents(
            student.getId(),
            new EventSelectionRequest(List.of(new EventSelection(event.id(), true)), 0L));
    assertThat(saved.changed()).isTrue();
    assertThat(service.participants(event.id(), 0, 10).totalElements()).isEqualTo(1);
    UUID campaign =
        jdbc.queryForObject(
            "select id from email_campaigns where created_by=?", UUID.class, student.getId());
    assertThat(campaigns.detail(campaign).status()).isEqualTo("COMPLETED");
    assertThat(campaigns.detail(campaign).recipients().getFirst().status()).isEqualTo("FAILED");
    doCallRealMethod().when(emailExecutor).execute(any(Runnable.class));
    campaigns.retry(admin.getId(), campaign, campaigns.detail(campaign).version(), null);
    awaitMail(student, 1);
  }

  @Test
  void longEventSummaryIsDeliveredAndNotificationFitsItsStorageLimit() throws Exception {
    User admin = account();
    User student = account();
    List<EventSelection> selections = new ArrayList<>();
    for (int i = 0; i < 8; i++)
      selections.add(
          new EventSelection(event(admin, "Uzun etkinlik adı " + "a".repeat(160) + i).id(), true));
    service.saveEvents(student.getId(), new EventSelectionRequest(selections, 0L));
    awaitMail(student, 1);
    assertThat(mail.messages.get(student.getEmail()).getFirst().length()).isGreaterThan(1000);
    assertThat(
            jdbc.queryForObject(
                "select length(message) from notifications where user_id=?",
                Integer.class,
                student.getId()))
        .isLessThanOrEqualTo(1000);
  }

  @Test
  void adminEditsMealDaysWithoutLosingRegistrationsAndRejectsStaleUpdates() throws Exception {
    User admin = account();
    User student = account();
    var week = week(admin);
    var first = week.days().getFirst();
    service.saveMeals(student.getId(), week.id(), new MealSelectionRequest(Set.of(first.id()), 0L));
    awaitMail(student, 1);
    var days =
        List.of(new MealDayRequest(first.date()), new MealDayRequest(week.weekStart().plusDays(1)));
    var request = new MealWeekUpdateRequest(days, 0L);
    String url = "/api/admin/meal-weeks/" + week.id();
    mvc.perform(
            put(url)
                .with(as(student, "USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
        .andExpect(status().isForbidden());
    mvc.perform(
            put(url)
                .with(as(admin, "ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scheduleVersion").value(1))
        .andExpect(jsonPath("$.days[0].id").value(first.id().toString()));
    assertThat(service.participants(first.id(), 0, 10).totalElements()).isEqualTo(1);
    mvc.perform(
            put(url)
                .with(as(admin, "ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ACTIVITY_VERSION_CONFLICT"));
    mvc.perform(
            put(url)
                .with(as(admin, "ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        new MealWeekUpdateRequest(
                            List.of(new MealDayRequest(week.weekStart().plusDays(1))), 1L))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("MEAL_DAY_HAS_PARTICIPANTS"));
    assertThat(service.week(week.id(), student.getId()).days().getFirst().attending()).isTrue();
    assertThat(campaignCount(student)).isEqualTo(1);
  }

  @Test
  void adminUpdatesEventAndNotifiesExistingParticipantsOnly() throws Exception {
    User admin = account();
    User student = account();
    User other = account();
    var event = event(admin, "Eski etkinlik");
    service.saveEvents(
        student.getId(),
        new EventSelectionRequest(List.of(new EventSelection(event.id(), true)), 0L));
    awaitMail(student, 1);
    var request =
        new EventUpdateRequest(
            "Yeni etkinlik",
            "Güncel açıklama",
            event.startsAt().plusSeconds(3600),
            "Yeni konum",
            0L);
    String url = "/api/admin/events/" + event.id();
    mvc.perform(
            put(url)
                .with(as(student, "USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
        .andExpect(status().isForbidden());
    mvc.perform(
            put(url)
                .with(as(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
        .andExpect(status().isForbidden());
    mvc.perform(
            put(url)
                .with(as(admin, "ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.title").value("Yeni etkinlik"));
    awaitMail(student, 2);
    assertThat(mail.messages.get(student.getEmail()).getLast())
        .contains("Yeni etkinlik", "Yeni konum", "Güncel açıklama");
    assertThat(mail.messages).doesNotContainKey(other.getEmail());
    assertThat(service.participants(event.id(), 0, 10).totalElements()).isEqualTo(1);
    mvc.perform(
            put(url)
                .with(as(admin, "ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
        .andExpect(status().isConflict());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_logs where action='EVENT_UPDATED' and entity_id=?",
                Integer.class,
                event.id()))
        .isEqualTo(1);
  }

  @Test
  void concurrentAdminEventUpdatesHaveExactlyOneWinner() throws Exception {
    User admin = account();
    var event = event(admin, "Paralel etkinlik");
    var request = new EventUpdateRequest("Güncel etkinlik", "", event.startsAt(), "", 0L);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      CountDownLatch start = new CountDownLatch(1);
      Callable<Integer> update =
          () -> {
            start.await();
            return mvc.perform(
                    put("/api/admin/events/" + event.id())
                        .with(as(admin, "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var first = executor.submit(update);
      var second = executor.submit(update);
      start.countDown();
      assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
    }
  }

  private User account() {
    User user =
        User.pending(
            UUID.randomUUID() + "@example.com",
            "unused-test-hash",
            "Ayşe",
            "Öğrenci",
            Instant.now());
    user.verify(Instant.now());
    return users.saveAndFlush(user);
  }

  private RequestPostProcessor as(User user, String role) {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthenticatedUser(user.getId(), user.getEmail(), role),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  private MealWeekResponse week(User admin) {
    LocalDate start =
        LocalDate.now()
            .plusWeeks(2)
            .with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    while (jdbc.queryForObject(
            "select count(*) from meal_weeks where week_start=?", Integer.class, start)
        > 0) start = start.plusWeeks(1);
    return service.createWeek(
        admin.getId(),
        new MealWeekRequest(
            start, List.of(new MealDayRequest(start), new MealDayRequest(start.plusDays(2)))),
        null);
  }

  private ActivityResponse event(User admin, String title) {
    return service.createEvent(
        admin.getId(),
        new EventRequest(title, "Açıklama", Instant.now().plusSeconds(864000), "İstanbul"),
        null);
  }

  private long campaignCount(User user) {
    return jdbc.queryForObject(
        "select count(*) from email_campaigns where created_by=?", Long.class, user.getId());
  }

  private void awaitMail(User user, int count) throws InterruptedException {
    for (int i = 0;
        i < 200
            && jdbc.queryForObject(
                    "select count(*) from email_recipients where user_id=? and status='SENT'",
                    Integer.class,
                    user.getId())
                < count;
        i++) Thread.sleep(20);
    assertThat(mail.messages.get(user.getEmail())).hasSize(count);
  }

  @TestConfiguration
  static class Config {
    @Bean
    @Primary
    Mail testMail() {
      return new Mail();
    }
  }

  static class Mail implements EmailSender {
    final Map<String, List<String>> messages = new ConcurrentHashMap<>();
    final Set<String> fail = ConcurrentHashMap.newKeySet();

    public void send(String email, String subject, String body) {
      if (fail.contains(email)) throw new IllegalStateException("Test SMTP failure");
      messages.computeIfAbsent(email, key -> new CopyOnWriteArrayList<>()).add(body);
    }
  }
}
