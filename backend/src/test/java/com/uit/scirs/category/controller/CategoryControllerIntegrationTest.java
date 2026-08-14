package com.uit.scirs.category.controller;

import com.uit.scirs.common.security.JwtUtil;
import com.uit.scirs.department.repository.DepartmentRepository;
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
class CategoryControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired DepartmentRepository departmentRepository;

    @Test
    void getAll_withStaffToken_returns200WithSeededCategories() throws Exception {
        String token = jwtUtil.generateToken(1L, "staff@example.com", RoleName.STAFF.name(), 1L);

        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$[?(@.name == 'Pothole / Damaged Road')]").exists());
    }

    @Test
    void create_withAdminTokenAndActiveDepartment_returns201() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);
        long roadsId = departmentRepository.findByName("Roads").orElseThrow().getId();

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Fallen Tree","description":"Fallen tree blocking road",
                                 "departmentId":%d,"icon":"tree","colorHex":"#16A34A"}""".formatted(roadsId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.departmentId").value(roadsId));
    }

    @Test
    void create_withInactiveDepartment_returns400() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);
        long parksId = departmentRepository.findByName("Parks").orElseThrow().getId();

        // Soft-delete the Parks department so the category creation must be rejected.
        mockMvc.perform(delete("/api/departments/" + parksId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Broken Bench","description":"Broken park bench",
                                 "departmentId":%d,"icon":"tree","colorHex":"#16A34A"}""".formatted(parksId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withUnknownDepartment_returns404() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mystery Issue","description":"No such department",
                                 "departmentId":999999,"icon":"tree","colorHex":"#16A34A"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withCitizenToken_returns403() throws Exception {
        String citizenToken = jwtUtil.generateToken(2L, "citizen@example.com", RoleName.CITIZEN.name(), null);
        long roadsId = departmentRepository.findByName("Roads").orElseThrow().getId();

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ghost Category","departmentId":%d}""".formatted(roadsId)))
                .andExpect(status().isForbidden());
    }
}
