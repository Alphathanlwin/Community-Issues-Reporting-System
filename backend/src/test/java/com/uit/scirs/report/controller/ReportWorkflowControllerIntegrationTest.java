package com.uit.scirs.report.controller;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.common.security.JwtUtil;
import com.uit.scirs.department.repository.DepartmentRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportWorkflowControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ReportRepository reportRepository;

    @Test
    void approve_thenResolve_awardsPointsAndAllowsFeedback() throws Exception {
        User citizen = persistCitizen("wf-citizen1@example.com");
        Category category = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
        User staff = persistStaff("wf-staff1@example.com", category.getDepartment().getId());
        User admin = adminUser();

        Report report = persistPendingReport(citizen, category);
        String adminToken = token(admin, RoleName.ADMIN, null);

        mockMvc.perform(patch("/api/reports/" + report.getId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.departmentId").value(category.getDepartment().getId()));

        String staffToken = token(staff, RoleName.STAFF, category.getDepartment().getId());
        mockMvc.perform(patch("/api/reports/" + report.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .contentType("application/json")
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // RESOLVED is blocked without a completion photo first.
        mockMvc.perform(patch("/api/reports/" + report.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .contentType("application/json")
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isBadRequest());

        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
        MockMultipartFile completionPhoto = new MockMultipartFile("images", "done.jpg", "image/jpeg", jpegBytes);
        mockMvc.perform(multipart("/api/reports/" + report.getId() + "/images")
                        .file(completionPhoto)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/reports/" + report.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .contentType("application/json")
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // Feedback can now be left by the original reporter.
        String citizenToken = token(citizen, RoleName.CITIZEN, null);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken)
                        .contentType("application/json")
                        .content("{\"reportId\":" + report.getId() + ",\"rating\":5,\"comment\":\"Great job!\"}"))
                .andExpect(status().isCreated());

        // Reporter's score reflects +10 (approved) + 20 (resolved) + 5 (feedback) = 35.
        mockMvc.perform(get("/api/score/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(35));

        // The reporter should have received a notification for each status change.
        mockMvc.perform(get("/api/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void reject_withoutReason_returns400() throws Exception {
        User citizen = persistCitizen("wf-citizen2@example.com");
        Category category = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
        Report report = persistPendingReport(citizen, category);
        String adminToken = token(adminUser(), RoleName.ADMIN, null);

        mockMvc.perform(patch("/api/reports/" + report.getId() + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_byStaffFromAnotherDepartment_returns403() throws Exception {
        User citizen = persistCitizen("wf-citizen3@example.com");
        Category category = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
        Long otherDepartmentId = departmentRepository.findByName("Water").orElseThrow().getId();
        User outsideStaff = persistStaff("wf-staff2@example.com", otherDepartmentId);

        Report report = persistPendingReport(citizen, category);
        mockMvc.perform(patch("/api/reports/" + report.getId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(adminUser(), RoleName.ADMIN, null)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/reports/" + report.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(outsideStaff, RoleName.STAFF, otherDepartmentId))
                        .contentType("application/json")
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
    }

    private Report persistPendingReport(User citizen, Category category) {
        Report report = new Report();
        report.setReportCode("RPT-TEST-" + System.nanoTime());
        report.setTitle("Pothole on Main St");
        report.setDescription("Large pothole blocking traffic");
        report.setCategory(category);
        report.setReporter(citizen);
        report.setStatus(ReportStatus.PENDING_APPROVAL);
        report.setPriority(ReportPriority.NORMAL);
        report.setLatitude(new BigDecimal("16.8409000"));
        report.setLongitude(new BigDecimal("96.1735000"));
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

    private User persistStaff(String email, Long departmentId) {
        Role staffRole = roleRepository.findByName(RoleName.STAFF).orElseThrow();
        User staff = new User();
        staff.setFullName("Test Staff");
        staff.setEmail(email);
        staff.setPasswordHash("bcrypt-hash");
        staff.setRole(staffRole);
        staff.setAccountStatus(AccountStatus.APPROVED);
        staff.setActive(true);
        staff.setDepartmentId(departmentId);
        return userRepository.save(staff);
    }

    private User adminUser() {
        return userRepository.findByEmail("admin@scirs.gov").orElseThrow();
    }

    // Takes the role/department explicitly rather than reading user.getRole()
    // — that relation is lazy and open-in-view is disabled, so it can only be
    // read inside the transaction that loaded it, not from a plain helper.
    private String token(User user, RoleName role, Long departmentId) {
        return jwtUtil.generateToken(user.getId(), user.getEmail(), role.name(), departmentId);
    }
}
