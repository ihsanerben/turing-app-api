package com.turing.app.api.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.turing.app.api.auth.service.AuthMailService;
import com.turing.app.api.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.Map;
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

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test")
@Import(ProfileControllerIT.TestMailConfig.class)
class ProfileControllerIT {
    @Container @ServiceConnection static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    @Autowired MockMvc mockMvc; @Autowired CapturingMailService mail; @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;

    @Test
    void isolatesUsersEncryptsSensitiveDataAndAuditsAdminCorrection() throws Exception {
        registerAndVerify("student@example.com");
        Cookie studentAccess = login("student@example.com");
        var studentId = users.findByEmailIgnoreCase("student@example.com").orElseThrow().getId();

        mockMvc.perform(get("/api/me/profile").cookie(studentAccess)).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(studentId.toString())).andExpect(jsonPath("$.id").doesNotExist());

        String createBody = """
                {"nationalId":"12345678901","birthDate":"2000-01-01","phone":"+905551112233",
                 "city":"İstanbul","countryCode":"tr","universityId":"10000000-0000-0000-0000-000000000001",
                 "departmentId":"20000000-0000-0000-0000-000000000001","educationLevel":"BACHELOR","studyYear":2,"gpa":3.45}
                """;
        mockMvc.perform(put("/api/me/profile").with(csrf()).cookie(studentAccess).contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.nationalId").value("12345678901"));

        byte[] encrypted = jdbc.queryForObject("select national_id_encrypted from student_profiles where user_id = ?", byte[].class, studentId);
        assertThat(encrypted).isNotNull(); assertThat(new String(encrypted)).doesNotContain("12345678901");

        mockMvc.perform(put("/api/me/profile").with(csrf()).cookie(studentAccess).contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PROFILE_VERSION_CONFLICT"));

        registerAndVerify("admin@example.com");
        var adminId = users.findByEmailIgnoreCase("admin@example.com").orElseThrow().getId();
        jdbc.update("update users set role = 'ADMIN' where id = ?", adminId);
        Cookie adminAccess = login("admin@example.com");

        mockMvc.perform(get("/api/admin/users/" + studentId + "/profile").cookie(studentAccess)).andExpect(status().isForbidden());
        String correction = """
                {"version":0,"nationalId":"12345678901","birthDate":"2000-01-01","phone":"+905551112233",
                 "city":"Ankara","countryCode":"TR","otherUniversity":"Başka Üniversite",
                 "otherDepartment":"Başka Bölüm","educationLevel":"BACHELOR","studyYear":3,"gpa":3.50}
                """;
        mockMvc.perform(put("/api/admin/users/" + studentId + "/profile").with(csrf()).cookie(adminAccess)
                        .contentType(MediaType.APPLICATION_JSON).content(correction))
                .andExpect(status().isOk()).andExpect(jsonPath("$.city").value("Ankara")).andExpect(jsonPath("$.version").value(1));
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where actor_id = ? and entity_id = (select id from student_profiles where user_id = ?)", Integer.class, adminId, studentId)).isEqualTo(1);
        String auditJson = jdbc.queryForObject("select new_values::text from audit_logs where actor_id = ?", String.class, adminId);
        assertThat(auditJson).doesNotContain("12345678901");
    }

    @Test
    void rejectsDepartmentFromAnotherUniversity() throws Exception {
        registerAndVerify("mismatch@example.com"); Cookie access = login("mismatch@example.com");
        mockMvc.perform(put("/api/me/profile").with(csrf()).cookie(access).contentType(MediaType.APPLICATION_JSON).content("""
                {"universityId":"10000000-0000-0000-0000-000000000001","departmentId":"20000000-0000-0000-0000-000000000002"}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DEPARTMENT_UNIVERSITY_MISMATCH"));
    }

    private void registerAndVerify(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(
                "{\"email\":\"" + email + "\",\"password\":\"strong-pass-123\",\"firstName\":\"Test\",\"lastName\":\"User\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/verify-email").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + mail.tokens.get(email) + "\"}")).andExpect(status().isOk());
    }
    private Cookie login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"strong-pass-123\"}"))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookie("TURING_ACCESS_TOKEN");
    }
    @TestConfiguration static class TestMailConfig { @Bean @Primary CapturingMailService mail() { return new CapturingMailService(); } }
    static class CapturingMailService implements AuthMailService {
        final Map<String, String> tokens = new ConcurrentHashMap<>();
        public void sendVerification(String email, String token) { tokens.put(email, token); }
        public void sendPasswordReset(String email, String token) {}
    }
}
