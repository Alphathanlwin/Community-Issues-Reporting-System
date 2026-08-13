package com.uit.scirs.auth.controller;

import com.uit.scirs.common.security.JwtUtil;
import com.uit.scirs.user.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(classes = {com.uit.scirs.ScirsApplication.class,
        AuthControllerIntegrationTest.AdminOnlyTestController.class})
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;

    @Test
    void login_seededAdmin_withCorrectPassword_returns200WithToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@scirs.gov","password":"Admin@12345"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_seededAdmin_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@scirs.gov","password":"wrongPassword"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_thenLoginBeforeApproval_returns403() throws Exception {
        String body = """
                {"fullName":"New Citizen","email":"newcitizen@example.com","phone":"+959111222333",
                 "password":"securePass123","dateOfBirth":"2000-01-01","nrcNumber":"12/ABC(N)999999"}""";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CITIZEN"))
                .andExpect(jsonPath("$.accountStatus").value("PENDING"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"newcitizen@example.com","password":"securePass123"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidAdminToken_returns200() throws Exception {
        String token = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@scirs.gov"));
    }

    @Test
    void adminOnlyEndpoint_withCitizenToken_returns403() throws Exception {
        String citizenToken = jwtUtil.generateToken(999L, "citizen@example.com", RoleName.CITIZEN.name(), null);

        mockMvc.perform(get("/api/test/admin-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnlyEndpoint_withAdminToken_returns200() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);

        mockMvc.perform(get("/api/test/admin-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    /**
     * Test-only endpoint used solely to verify that role-gated routes are
     * correctly enforced by SecurityConfig; no equivalent exists in main
     * source until an admin-only module (e.g. user management) is built.
     */
    @RestController
    static class AdminOnlyTestController {

        @GetMapping("/api/test/admin-only")
        @PreAuthorize("hasRole('ADMIN')")
        public String adminOnly() {
            return "ok";
        }
    }
}
