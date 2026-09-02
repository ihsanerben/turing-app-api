package com.turing.app.api.application.controller;

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

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test") @Import(ApplicationControllerIT.MailConfig.class)
class ApplicationControllerIT {
    @Container @ServiceConnection static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
    @Autowired MockMvc mvc; @Autowired Mail mail; @Autowired UserRepository users; @Autowired JdbcTemplate jdbc;

    @Test
    void protectsOwnershipValidatesTypedAnswersAndSubmitsAtomically() throws Exception {
        Cookie admin=session("application-admin@example.com");UUID adminId=users.findByEmailIgnoreCase("application-admin@example.com").orElseThrow().getId();jdbc.update("update users set role='ADMIN' where id=?",adminId);admin=sessionLogin("application-admin@example.com");
        Cookie student=session("application-student@example.com");createProfile(student);
        Cookie other=session("application-other@example.com");createProfile(other);
        String periodId=provisionPeriod(admin);

        MvcResult created=mvc.perform(post("/api/me/applications").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content("{\"periodId\":\""+periodId+"\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.completion").value(0)).andReturn();
        String applicationId=JsonPath.read(created.getResponse().getContentAsString(),"$.id");
        mvc.perform(post("/api/me/applications").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content("{\"periodId\":\""+periodId+"\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("APPLICATION_ALREADY_EXISTS"));
        mvc.perform(get("/api/me/applications/"+applicationId).cookie(other)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));

        MvcResult schema=mvc.perform(get("/api/me/applications/"+applicationId+"/form").cookie(student)).andExpect(status().isOk()).andExpect(jsonPath("$.form.versionNumber").value(1)).andReturn();
        List<String> fieldIds=JsonPath.read(schema.getResponse().getContentAsString(),"$.form.sections[0].fields[*].id");
        String motivationId=fieldIds.get(0),incomeId=fieldIds.get(1),housingId=fieldIds.get(2),skillsId=fieldIds.get(3);
        mvc.perform(post("/api/me/applications/"+applicationId+"/submit").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUIRED_ANSWERS_MISSING"));
        assertThat(jdbc.queryForObject("select count(*) from application_snapshots",Integer.class)).isZero();

        String invalid="{\"version\":0,\"answers\":[{\"fieldId\":\""+housingId+"\",\"value\":\"invalid\"}]}";
        mvc.perform(put("/api/me/applications/"+applicationId+"/answers").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content(invalid))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ANSWER"));
        String answers="""
            {"version":0,"answers":[
              {"fieldId":"%s","value":"Topluma katkı sağlayacak projeler üretmek istiyorum."},
              {"fieldId":"%s","value":12500.50},
              {"fieldId":"%s","value":"dorm"},
              {"fieldId":"%s","value":["java","react"]}
            ]}
            """.formatted(motivationId,incomeId,housingId,skillsId);
        mvc.perform(put("/api/me/applications/"+applicationId+"/answers").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content(answers))
            .andExpect(status().isOk()).andExpect(jsonPath("$.application.completion").value(100)).andExpect(jsonPath("$.application.version").value(1));
        assertThat(jdbc.queryForObject("select count(*) from application_answers where text_value is not null",Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from application_answers where number_value is not null",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from application_answers where json_value is not null",Integer.class)).isEqualTo(1);
        mvc.perform(put("/api/me/applications/"+applicationId+"/answers").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content(answers))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));

        mvc.perform(post("/api/me/applications/"+applicationId+"/submit").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED")).andExpect(jsonPath("$.version").value(2));
        mvc.perform(put("/api/me/applications/"+applicationId+"/answers").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content(answers.replace("\"version\":0","\"version\":2")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("APPLICATION_IMMUTABLE"));
        String snapshot=jdbc.queryForObject("select profile_data::text from application_snapshots",String.class);assertThat(snapshot).contains("application-student@example.com").doesNotContain("12345678901");
        assertThat(jdbc.queryForObject("select count(*) from application_status_history where application_id=?",Integer.class,UUID.fromString(applicationId))).isEqualTo(2);
        mvc.perform(post("/api/me/applications/"+applicationId+"/withdraw").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content("{\"version\":2}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WITHDRAWN"));
        jdbc.update("update application_periods set starts_at=now()-interval '2 days', ends_at=now()-interval '1 day' where id=?",UUID.fromString(periodId));
        mvc.perform(post("/api/me/applications").with(csrf()).cookie(other).contentType(MediaType.APPLICATION_JSON).content("{\"periodId\":\""+periodId+"\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("APPLICATION_PERIOD_CLOSED"));
    }

    @Test
    void adminListsFiltersNotesAndTransitionsApplicationsWithoutLeakingInternalsToStudent() throws Exception {
        Cookie admin=session("management-admin@example.com");UUID adminId=users.findByEmailIgnoreCase("management-admin@example.com").orElseThrow().getId();jdbc.update("update users set role='ADMIN' where id=?",adminId);admin=sessionLogin("management-admin@example.com");Cookie student=session("management-student@example.com");createProfile(student);String periodId=provisionPeriod(admin);
        String applicationId=JsonPath.read(mvc.perform(post("/api/me/applications").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content("{\"periodId\":\""+periodId+"\"}")).andReturn().getResponse().getContentAsString(),"$.id");MvcResult schema=mvc.perform(get("/api/me/applications/"+applicationId+"/form").cookie(student)).andReturn();List<String> ids=JsonPath.read(schema.getResponse().getContentAsString(),"$.form.sections[0].fields[*].id");String answers="{\"version\":0,\"answers\":[{\"fieldId\":\""+ids.get(0)+"\",\"value\":\"Yeterince uzun motivasyon metni\"},{\"fieldId\":\""+ids.get(1)+"\",\"value\":1000},{\"fieldId\":\""+ids.get(2)+"\",\"value\":\"dorm\"}]}";mvc.perform(put("/api/me/applications/"+applicationId+"/answers").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content(answers)).andExpect(status().isOk());mvc.perform(post("/api/me/applications/"+applicationId+"/submit").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/applications").cookie(student)).andExpect(status().isForbidden());mvc.perform(get("/api/admin/applications?search=management-student&status=SUBMITTED&sort=submittedAt&direction=asc").cookie(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].studentEmail").value("management-student@example.com"));mvc.perform(post("/api/admin/applications/"+applicationId+"/notes").with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Kimlik kontrolü tamamlandı.\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.content").value("Kimlik kontrolü tamamlandı."));mvc.perform(get("/api/me/applications/"+applicationId).cookie(student)).andExpect(status().isOk()).andExpect(jsonPath("$.notes").doesNotExist());mvc.perform(patch("/api/admin/applications/"+applicationId+"/status").with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\",\"version\":2,\"reason\":\"Ön inceleme başladı.\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.application.status").value("UNDER_REVIEW")).andExpect(jsonPath("$.history[0].reason").value("Ön inceleme başladı."));mvc.perform(patch("/api/admin/applications/"+applicationId+"/status").with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\",\"version\":3,\"reason\":\"Atlama\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_APPLICATION_TRANSITION"));mvc.perform(patch("/api/admin/applications/"+applicationId+"/status").with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"MISSING_DOCUMENT\",\"version\":3,\"reason\":\"Belge yeniden yüklenmeli.\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.application.status").value("MISSING_DOCUMENT"));mvc.perform(put("/api/me/applications/"+applicationId+"/answers").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content(answers.replace("\"version\":0","\"version\":4"))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("APPLICATION_IMMUTABLE"));mvc.perform(post("/api/me/applications/"+applicationId+"/submit").with(csrf()).cookie(student).contentType(MediaType.APPLICATION_JSON).content("{\"version\":4}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"));assertThat(jdbc.queryForObject("select count(*) from audit_logs where entity_id=? and action in ('APPLICATION_NOTE_ADDED','APPLICATION_STATUS_CHANGED')",Integer.class,UUID.fromString(applicationId))).isEqualTo(3);
    }

    private String provisionPeriod(Cookie admin) throws Exception {String slug="basari-bursu-"+UUID.randomUUID();String programId=JsonPath.read(mvc.perform(post("/api/admin/scholarship-programs").with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Başarı Bursu\",\"slug\":\""+slug+"\",\"description\":\"Öğrenci bursu\"}")).andReturn().getResponse().getContentAsString(),"$.id");Instant now=Instant.now();String periodId=JsonPath.read(mvc.perform(post("/api/admin/application-periods").with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"programId\":\""+programId+"\",\"name\":\"2026 Başvuruları\",\"academicYear\":\"2026-2027\",\"startsAt\":\""+now.minusSeconds(60)+"\",\"endsAt\":\""+now.plusSeconds(86400)+"\",\"maxRecipients\":20,\"allowWithdrawal\":true}")).andReturn().getResponse().getContentAsString(),"$.id");String formId=JsonPath.read(mvc.perform(post("/api/admin/application-periods/"+periodId+"/forms").with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Başvuru Formu\"}")).andReturn().getResponse().getContentAsString(),"$.id");String formSchema="""
            {"version":0,"name":"Başvuru Formu","sections":[{"title":"Başvuru Bilgileri","description":null,"fields":[
              {"key":"motivation","label":"Motivasyon","type":"TEXTAREA","required":true,"placeholder":null,"validationRules":{"minLength":10},"options":[]},
              {"key":"income","label":"Gelir","type":"DECIMAL","required":true,"placeholder":null,"validationRules":{"min":0},"options":[]},
              {"key":"housing","label":"Barınma","type":"SELECT","required":true,"placeholder":null,"validationRules":{},"options":[{"label":"Yurt","value":"dorm"},{"label":"Aile","value":"family"}]},
              {"key":"skills","label":"Yetkinlikler","type":"MULTI_SELECT","required":false,"placeholder":null,"validationRules":{},"options":[{"label":"Java","value":"java"},{"label":"React","value":"react"}]}
            ]}]}
            """;mvc.perform(put("/api/admin/forms/"+formId+"/schema").with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON).content(formSchema)).andExpect(status().isOk());mvc.perform(post("/api/admin/forms/"+formId+"/publish").with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}")).andExpect(status().isOk());mvc.perform(patch("/api/admin/application-periods/"+periodId+"/status").with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"status\":\"OPEN\"}")).andExpect(status().isOk());return periodId;}
    private void createProfile(Cookie cookie) throws Exception {mvc.perform(put("/api/me/profile").with(csrf()).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"nationalId\":\"12345678901\",\"birthDate\":\"2000-01-01\",\"phone\":\"+905551112233\",\"city\":\"İstanbul\",\"countryCode\":\"TR\",\"universityId\":\"10000000-0000-0000-0000-000000000001\",\"departmentId\":\"20000000-0000-0000-0000-000000000001\",\"educationLevel\":\"BACHELOR\",\"studyYear\":2,\"gpa\":3.45}")).andExpect(status().isOk());}
    private Cookie session(String email) throws Exception {mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"strong-pass-123\",\"firstName\":\"Test\",\"lastName\":\"Student\"}")).andExpect(status().isCreated());mvc.perform(post("/api/auth/verify-email").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+mail.tokens.get(email)+"\"}")).andExpect(status().isOk());return sessionLogin(email);}
    private Cookie sessionLogin(String email) throws Exception {return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"strong-pass-123\"}")).andExpect(status().isOk()).andReturn().getResponse().getCookie("TURING_ACCESS_TOKEN");}
    @TestConfiguration static class MailConfig {@Bean @Primary Mail mail(){return new Mail();}}
    static class Mail implements AuthMailService {final Map<String,String> tokens=new ConcurrentHashMap<>();public void sendVerification(String email,String token){tokens.put(email,token);}public void sendPasswordReset(String email,String token){}}
}
