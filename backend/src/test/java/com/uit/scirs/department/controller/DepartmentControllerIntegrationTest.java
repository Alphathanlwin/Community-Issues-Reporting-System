package com.uit.scirs.department.controller;

import com.uit.scirs.common.security.JwtUtil;
import com.uit.scirs.user.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepartmentControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;

    @Test
    void getAll_withCitizenToken_returns200WithSeededDepartments() throws Exception {
        String token = jwtUtil.generateToken(1L, "citizen@example.com", RoleName.CITIZEN.name(), null);

        mockMvc.perform(get("/api/departments").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$[?(@.name == 'Roads')]").exists());
    }

    @Test
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/departments")).andExpect(status().isUnauthorized());
    }

    @Test
    void create_withAdminToken_returns201() throws Exception {
        String token = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);

        mockMvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Traffic","description":"Traffic control","contactEmail":"traffic@scirs.gov"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Traffic"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_withCitizenToken_returns403() throws Exception {
        String token = jwtUtil.generateToken(2L, "citizen2@example.com", RoleName.CITIZEN.name(), null);

        mockMvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ghost Department"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withStaffToken_returns403() throws Exception {
        String token = jwtUtil.generateToken(3L, "staff@example.com", RoleName.STAFF.name(), 1L);

        mockMvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ghost Department 2"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withDuplicateSeededName_returns409() throws Exception {
        String token = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);

        mockMvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Roads","description":"dup","contactEmail":"dup@scirs.gov"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_withAdminToken_softDeletesDepartment() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);

        String response = mockMvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Temporary Dept","description":"temp","contactEmail":"temp@scirs.gov"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number idNode = com.jayway.jsonpath.JsonPath.read(response, "$.id");
        long id = idNode.longValue();

        mockMvc.perform(delete("/api/departments/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/departments/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
