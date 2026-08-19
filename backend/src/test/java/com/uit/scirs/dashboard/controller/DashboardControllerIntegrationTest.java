package com.uit.scirs.dashboard.controller;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.common.security.JwtUtil;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.repository.DepartmentRepository;
import com.uit.scirs.report.entity.Report;
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
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ReportRepository reportRepository;

    @Test
    void adminDashboard_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminDashboard_withCitizenToken_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDashboard_withStaffToken_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken(roadsDepartment().getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDashboard_withAdminToken_reportsPendingCitizenAndReport() throws Exception {
        persistCitizen("pending-citizen@example.com", AccountStatus.PENDING);
        User reporter = persistCitizen("reporter-admin-dash@example.com", AccountStatus.APPROVED);
        persistReport(reporter, potholeCategory(), null, ReportStatus.PENDING_APPROVAL, null, null);

        mockMvc.perform(get("/api/dashboard/admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAccountCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.pendingReportCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.reportsAwaitingApproval").isArray());
    }

    @Test
    void staffDashboard_withCitizenToken_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/staff").header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffDashboard_scopesCountsToTheStaffMembersOwnDepartment() throws Exception {
        Department roads = roadsDepartment();
        Category pothole = potholeCategory();
        User reporter = persistCitizen("reporter-staff-dash@example.com", AccountStatus.APPROVED);

        persistReport(reporter, pothole, roads, ReportStatus.ASSIGNED, LocalDateTime.now().minusDays(1), null);
        LocalDateTime approvedAt = LocalDateTime.now().minusDays(2);
        persistReport(reporter, pothole, roads, ReportStatus.RESOLVED, approvedAt, approvedAt.plusHours(5));

        mockMvc.perform(get("/api/dashboard/staff").header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken(roads.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.resolvedReports").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.newReports").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.monthlySeries.length()").value(12));
    }

    @Test
    void departmentPerformance_withStaffToken_includesEveryDepartment() throws Exception {
        mockMvc.perform(get("/api/dashboard/departments").header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken(roadsDepartment().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$[?(@.departmentName == 'Roads')]").exists());
    }

    @Test
    void departmentPerformance_withCitizenToken_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/departments").header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void categoryVolume_withAdminToken_includesEveryCategory() throws Exception {
        mockMvc.perform(get("/api/dashboard/categories").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$[?(@.categoryName == 'Pothole / Damaged Road')]").exists());
    }

    @Test
    void categoryVolume_withCitizenToken_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/categories").header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken()))
                .andExpect(status().isForbidden());
    }

    private String adminToken() {
        User admin = userRepository.findByEmail("admin@scirs.gov").orElseThrow();
        return jwtUtil.generateToken(admin.getId(), admin.getEmail(), RoleName.ADMIN.name(), null);
    }

    private String citizenToken() {
        User citizen = persistCitizen("dash-citizen-" + System.nanoTime() + "@example.com", AccountStatus.APPROVED);
        return jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);
    }

    private String staffToken(Long departmentId) {
        return jwtUtil.generateToken(1L, "staff-dash@example.com", RoleName.STAFF.name(), departmentId);
    }

    private Department roadsDepartment() {
        return departmentRepository.findByName("Roads").orElseThrow();
    }

    private Category potholeCategory() {
        return categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
    }

    private User persistCitizen(String email, AccountStatus status) {
        Role citizenRole = roleRepository.findByName(RoleName.CITIZEN).orElseThrow();

        User citizen = new User();
        citizen.setFullName("Test Citizen");
        citizen.setEmail(email);
        citizen.setPasswordHash("bcrypt-hash");
        citizen.setRole(citizenRole);
        citizen.setAccountStatus(status);
        citizen.setActive(true);
        return userRepository.save(citizen);
    }

    private Report persistReport(User reporter, Category category, Department department, ReportStatus status,
                                  LocalDateTime approvedAt, LocalDateTime resolvedAt) {
        Report report = new Report();
        report.setReportCode("RPT-TEST-" + System.nanoTime());
        report.setTitle("Test report");
        report.setDescription("Test report description");
        report.setCategory(category);
        report.setDepartment(department);
        report.setReporter(reporter);
        report.setStatus(status);
        report.setLatitude(new BigDecimal("16.8409000"));
        report.setLongitude(new BigDecimal("96.1735000"));
        report.setApprovedAt(approvedAt);
        report.setResolvedAt(resolvedAt);
        return reportRepository.save(report);
    }
}
