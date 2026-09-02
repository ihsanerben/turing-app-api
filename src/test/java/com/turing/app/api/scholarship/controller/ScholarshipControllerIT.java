package com.turing.app.api.scholarship.controller;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.turing.app.api.auth.service.AuthMailService;
import com.turing.app.api.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.*;import org.springframework.boot.testcontainers.service.connection.ServiceConnection;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.context.annotation.*;import org.springframework.http.MediaType;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.test.context.ActiveProfiles;import org.springframework.test.web.servlet.*;import org.testcontainers.containers.PostgreSQLContainer;import org.testcontainers.junit.jupiter.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test") @Import(ScholarshipControllerIT.MailConfig.class)
class ScholarshipControllerIT{
 @Container @ServiceConnection static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 @Autowired MockMvc mvc;@Autowired Mail mail;@Autowired UserRepository users;@Autowired JdbcTemplate jdbc;
 @Test void enforcesRoleDatesLifecycleVersionArchiveAndPublicVisibility()throws Exception{
  register();var id=users.findByEmailIgnoreCase("admin-scholarship@example.com").orElseThrow().getId();jdbc.update("update users set role='ADMIN' where id=?",id);Cookie access=login();
  MvcResult created=mvc.perform(post("/api/admin/scholarship-programs").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("""
   {"name":"Gelecek Bursu","slug":"gelecek-bursu","description":"Başarılı öğrenciler için burs."}
   """)).andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(0)).andReturn();
  String programId=com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(),"$.id");
  Instant now=Instant.now();String period="{\"programId\":\""+programId+"\",\"name\":\"2026 Başvuruları\",\"academicYear\":\"2026-2027\",\"startsAt\":\""+now.minusSeconds(60)+"\",\"endsAt\":\""+now.plusSeconds(86400)+"\",\"maxRecipients\":20,\"allowWithdrawal\":true}";
  MvcResult periodResult=mvc.perform(post("/api/admin/application-periods").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content(period)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn();
  String periodId=com.jayway.jsonpath.JsonPath.read(periodResult.getResponse().getContentAsString(),"$.id");
  MvcResult formResult=mvc.perform(post("/api/admin/application-periods/"+periodId+"/forms").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Başvuru Formu\"}" )).andExpect(status().isCreated()).andReturn();
  String formId=com.jayway.jsonpath.JsonPath.read(formResult.getResponse().getContentAsString(),"$.id");
  mvc.perform(put("/api/admin/forms/"+formId+"/schema").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"name\":\"Başvuru Formu\",\"sections\":[{\"title\":\"Genel\",\"description\":null,\"fields\":[{\"key\":\"motivation\",\"label\":\"Motivasyon\",\"type\":\"TEXTAREA\",\"required\":true,\"placeholder\":null,\"validationRules\":{},\"options\":[]}]}]}" )).andExpect(status().isOk());
  mvc.perform(post("/api/admin/forms/"+formId+"/publish").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}" )).andExpect(status().isOk());
  mvc.perform(patch("/api/admin/application-periods/"+periodId+"/status").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"status\":\"OPEN\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
  mvc.perform(patch("/api/admin/application-periods/"+periodId+"/status").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("{\"version\":1,\"status\":\"SCHEDULED\"}"))
    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_PERIOD_TRANSITION"));
  mvc.perform(post("/api/admin/scholarship-programs/"+programId+"/archive").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PROGRAM_HAS_ACTIVE_PERIOD"));
  mvc.perform(get("/api/public/scholarships")).andExpect(status().isOk()).andExpect(jsonPath("$[0].program.slug").value("gelecek-bursu")).andExpect(jsonPath("$[0].periods[0].status").value("OPEN"));
  mvc.perform(put("/api/admin/scholarship-programs/"+programId).with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("{\"version\":99,\"name\":\"X\",\"slug\":\"x\",\"description\":\"Y\"}"))
    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
  org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from audit_logs where actor_id=? and entity_type in ('SCHOLARSHIP_PROGRAM','APPLICATION_PERIOD')",Integer.class,id)).isEqualTo(3);
 }
 private void register()throws Exception{mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin-scholarship@example.com\",\"password\":\"strong-pass-123\",\"firstName\":\"Admin\",\"lastName\":\"User\"}"));mvc.perform(post("/api/auth/verify-email").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+mail.token.get()+"\"}"));}
 private Cookie login()throws Exception{return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin-scholarship@example.com\",\"password\":\"strong-pass-123\"}")).andExpect(status().isOk()).andReturn().getResponse().getCookie("TURING_ACCESS_TOKEN");}
 @TestConfiguration static class MailConfig{@Bean @Primary Mail mail(){return new Mail();}}static class Mail implements AuthMailService{final AtomicReference<String> token=new AtomicReference<>();public void sendVerification(String e,String t){token.set(t);}public void sendPasswordReset(String e,String t){}}
}
