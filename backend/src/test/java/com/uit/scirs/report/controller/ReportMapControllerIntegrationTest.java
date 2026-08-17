package com.uit.scirs.report.controller;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.common.security.JwtUtil;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportPriority;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.Role;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.RoleRepository;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportMapControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ReportRepository reportRepository;

    @Test
    void getMapPins_asCitizen_excludesPendingApprovalAndRejected() throws Exception {
        User citizen = persistCitizen("map-citizen1@example.com");
        Category category = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();

        Report pending = persistReport(citizen, category, ReportStatus.PENDING_APPROVAL, "16.8000000", "96.1000000");
        Report rejected = persistReport(citizen, category, ReportStatus.REJECTED, "16.8000000", "96.1000000");
        Report assigned = persistReport(citizen, category, ReportStatus.ASSIGNED, "16.8000000", "96.1000000");

        String token = token(citizen, RoleName.CITIZEN, null);

        mockMvc.perform(get("/api/reports/map").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + pending.getId() + ")]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == " + rejected.getId() + ")]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == " + assigned.getId() + ")]").exists());
    }

    @Test
    void getMapPins_asAdmin_includesPendingApprovalAndRejected() throws Exception {
        User citizen = persistCitizen("map-citizen2@example.com");
        Category category = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();

        Report pending = persistReport(citizen, category, ReportStatus.PENDING_APPROVAL, "16.8000000", "96.1000000");
        String adminToken = token(adminUser(), RoleName.ADMIN, null);

        mockMvc.perform(get("/api/reports/map").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + pending.getId() + ")]").exists());
    }

    @Test
    void getMapPins_returnsSlimPayloadShape() throws Exception {
        User citizen = persistCitizen("map-citizen3@example.com");
        Category category = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
        Report report = persistReport(citizen, category, ReportStatus.ASSIGNED, "16.8409000", "96.1735000");

        mockMvc.perform(get("/api/reports/map").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(citizen, RoleName.CITIZEN, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + report.getId() + ")].reportCode").exists())
                .andExpect(jsonPath("$[?(@.id == " + report.getId() + ")].categoryName").value(category.getName()))
                .andExpect(jsonPath("$[?(@.id == " + report.getId() + ")].status").value("ASSIGNED"));
    }

    @Test
    void getMapPins_filteredByBoundingBox_excludesReportsOutsideBounds() throws Exception {
        User citizen = persistCitizen("map-citizen4@example.com");
        Category category = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();

        Report inside = persistReport(citizen, category, ReportStatus.ASSIGNED, "16.8000000", "96.1000000");
        Report outside = persistReport(citizen, category, ReportStatus.ASSIGNED, "20.0000000", "100.0000000");

        String token = token(citizen, RoleName.CITIZEN, null);

        mockMvc.perform(get("/api/reports/map")
                        .param("minLat", "16.0").param("maxLat", "17.0")
                        .param("minLng", "95.0").param("maxLng", "97.0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + inside.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + outside.getId() + ")]").isEmpty());
    }

    @Test
    void getMapPins_filteredByStatus_returnsOnlyMatchingStatus() throws Exception {
        User citizen = persistCitizen("map-citizen5@example.com");
        Category category = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();

        Report assigned = persistReport(citizen, category, ReportStatus.ASSIGNED, "16.8000000", "96.1000000");
        Report inProgress = persistReport(citizen, category, ReportStatus.IN_PROGRESS, "16.8000000", "96.1000000");

        String token = token(citizen, RoleName.CITIZEN, null);

        mockMvc.perform(get("/api/reports/map").param("status", "ASSIGNED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + assigned.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + inProgress.getId() + ")]").isEmpty());
    }

    @Test
    void getMapPins_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/reports/map"))
                .andExpect(status().isUnauthorized());
    }

    private Report persistReport(User citizen, Category category, ReportStatus status, String lat, String lng) {
        Report report = new Report();
        report.setReportCode("RPT-MAP-" + System.nanoTime());
        report.setTitle("Pothole on Main St");
        report.setDescription("Large pothole blocking traffic");
        report.setCategory(category);
        report.setReporter(citizen);
        report.setStatus(status);
        report.setPriority(ReportPriority.NORMAL);
        report.setLatitude(new BigDecimal(lat));
        report.setLongitude(new BigDecimal(lng));
        return reportRepository.save(report);
    }

    private User persistCitizen(String email) {
        Role citizenRole = roleRepository.findByName(RoleName.CITIZEN).orElseThrow();
        User citizen = new User();
        citizen.setFullName("Test Citizen");
        citizen.setEmail(email);
        citizen.setPasswordHash("bcrypt-hash");
        citizen.setRole(citizenRole);
        citizen.setAccountStatus(AccountStatus.APPROVED);
        citizen.setActive(true);
        return userRepository.save(citizen);
    }

    private User adminUser() {
        return userRepository.findByEmail("admin@scirs.gov").orElseThrow();
    }

    private String token(User user, RoleName role, Long departmentId) {
        return jwtUtil.generateToken(user.getId(), user.getEmail(), role.name(), departmentId);
    }
}
