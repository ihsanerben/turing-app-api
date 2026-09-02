package com.turing.app.api.scholarship.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.turing.app.api.auth.service.AuthMailService;
import com.turing.app.api.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test") @Import(FormControllerIT.MailConfig.class)
class FormControllerIT {
    @Container @ServiceConnection static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
    @Autowired MockMvc mvc; @Autowired Mail mail; @Autowired UserRepository users; @Autowired JdbcTemplate jdbc;

    @Test
    void versionsFormSchemaAndKeepsPublishedVersionsImmutable() throws Exception {
        Cookie access=adminSession();
        String programId=JsonPath.read(mvc.perform(post("/api/admin/scholarship-programs").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"Bilim Bursu","slug":"bilim-bursu","description":"Bilim öğrencileri için burs."}
            """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),"$.id");
        Instant now=Instant.now();
        String periodId=JsonPath.read(mvc.perform(post("/api/admin/application-periods").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("""
            {"programId":"%s","name":"2027 Başvuruları","academicYear":"2027-2028","startsAt":"%s","endsAt":"%s","maxRecipients":10,"allowWithdrawal":true}
            """.formatted(programId,now.plusSeconds(86400),now.plusSeconds(172800)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),"$.id");

        MvcResult created=mvc.perform(post("/api/admin/application-periods/"+periodId+"/forms").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Başvuru Formu\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.versionNumber").value(1)).andExpect(jsonPath("$.status").value("DRAFT")).andReturn();
        String formId=JsonPath.read(created.getResponse().getContentAsString(),"$.id");
        String schema="""
            {"version":0,"name":"Başvuru Formu","sections":[{"title":"Ekonomik Bilgiler","description":"Güncel durum","fields":[
              {"key":"family_income","label":"Aile geliri","type":"DECIMAL","required":true,"placeholder":"Aylık gelir","validationRules":{"min":0},"options":[]},
              {"key":"housing","label":"Barınma şekli","type":"SELECT","required":true,"placeholder":null,"validationRules":{},"options":[{"label":"Yurt","value":"dorm"},{"label":"Aile yanı","value":"family"}]}
            ]}]}
            """;
        mvc.perform(put("/api/admin/forms/"+formId+"/schema").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content(schema))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.sections[0].fields[1].options[0].value").value("dorm"));
        mvc.perform(post("/api/admin/forms/"+formId+"/publish").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED")).andExpect(jsonPath("$.version").value(2));
        mvc.perform(put("/api/admin/forms/"+formId+"/schema").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content(schema.replace("\"version\":0","\"version\":2")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("FORM_IMMUTABLE"));

        MvcResult copied=mvc.perform(post("/api/admin/forms/"+formId+"/new-version").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("{\"version\":2}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.versionNumber").value(2)).andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.sections[0].fields[0].key").value("family_income")).andReturn();
        String copyId=JsonPath.read(copied.getResponse().getContentAsString(),"$.id");
        mvc.perform(post("/api/admin/forms/"+formId+"/new-version").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("{\"version\":2}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("FORM_DRAFT_ALREADY_EXISTS"));
        mvc.perform(put("/api/admin/forms/"+copyId+"/schema").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content(schema.replace("\"version\":0","\"version\":99")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
        mvc.perform(get("/api/admin/application-periods/"+periodId+"/forms").cookie(access)).andExpect(status().isOk()).andExpect(jsonPath("$[0].versionNumber").value(2)).andExpect(jsonPath("$[1].status").value("PUBLISHED"));
        mvc.perform(get("/api/admin/forms/"+formId)).andExpect(status().isUnauthorized());
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from form_fields",Integer.class)).isEqualTo(4);
    }

    private Cookie adminSession() throws Exception {mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin-form@example.com\",\"password\":\"strong-pass-123\",\"firstName\":\"Admin\",\"lastName\":\"Form\"}"));mvc.perform(post("/api/auth/verify-email").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+mail.token.get()+"\"}"));var id=users.findByEmailIgnoreCase("admin-form@example.com").orElseThrow().getId();jdbc.update("update users set role='ADMIN' where id=?",id);return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin-form@example.com\",\"password\":\"strong-pass-123\"}")).andExpect(status().isOk()).andReturn().getResponse().getCookie("TURING_ACCESS_TOKEN");}
    @TestConfiguration static class MailConfig {@Bean @Primary Mail mail(){return new Mail();}}
    static class Mail implements AuthMailService {final AtomicReference<String> token=new AtomicReference<>();public void sendVerification(String email,String value){token.set(value);}public void sendPasswordReset(String email,String value){}}
}
