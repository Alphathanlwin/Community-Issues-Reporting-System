package com.uit.scirs.report.controller;

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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ReportRepository reportRepository;

    @Test
    void createReport_withApprovedCitizenAndValidData_returns201WithPendingApprovalStatus() throws Exception {
        User citizen = persistApprovedCitizen("citizen1@example.com");
        String token = jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);
        long categoryId = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow().getId();

        MockMultipartFile data = jsonPart(categoryId, "16.8409000", "96.1735000");

        mockMvc.perform(multipart("/api/reports").file(data)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.reporterId").value(citizen.getId()))
                .andExpect(jsonPath("$.categoryId").value(categoryId));
    }

    @Test
    void createReport_withImage_returns201AndStoresImageUrl() throws Exception {
        User citizen = persistApprovedCitizen("citizen2@example.com");
        String token = jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);
        long categoryId = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow().getId();

        MockMultipartFile data = jsonPart(categoryId, "16.8409000", "96.1735000");
        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
        MockMultipartFile image = new MockMultipartFile("images", "pothole.jpg", "image/jpeg", jpegBytes);

        mockMvc.perform(multipart("/api/reports").file(data).file(image)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.images[0].imageUrl").exists());
    }

    @Test
    void createReport_withInvalidImageContent_returns400() throws Exception {
        User citizen = persistApprovedCitizen("citizen3@example.com");
        String token = jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);
        long categoryId = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow().getId();

        MockMultipartFile data = jsonPart(categoryId, "16.8409000", "96.1735000");
        MockMultipartFile bogusImage = new MockMultipartFile("images", "fake.jpg", "image/jpeg",
                "not a real image".getBytes());

        mockMvc.perform(multipart("/api/reports").file(data).file(bogusImage)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReport_withImageOverFiveMegabytes_returns400() throws Exception {
        // MockMvc's simulated multipart parsing does not enforce
        // spring.servlet.multipart.max-file-size the way a real servlet
        // container does (that produces a 413 in production via
        // MaxUploadSizeExceededException), so this exercises
        // FileStorageService's own belt-and-braces size check instead.
        User citizen = persistApprovedCitizen("citizen8@example.com");
        String token = jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);
        long categoryId = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow().getId();

        MockMultipartFile data = jsonPart(categoryId, "16.8409000", "96.1735000");
        byte[] oversized = new byte[6 * 1024 * 1024];
        MockMultipartFile hugeImage = new MockMultipartFile("images", "huge.jpg", "image/jpeg", oversized);

        mockMvc.perform(multipart("/api/reports").file(data).file(hugeImage)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReport_withUnknownCategory_returns404() throws Exception {
        User citizen = persistApprovedCitizen("citizen4@example.com");
        String token = jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);

        MockMultipartFile data = jsonPart(999999L, "16.8409000", "96.1735000");

        mockMvc.perform(multipart("/api/reports").file(data)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReport_withBlankTitle_returns400WithFieldError() throws Exception {
        User citizen = persistApprovedCitizen("citizen5@example.com");
        String token = jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);
        long categoryId = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow().getId();

        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", ("""
                {"title":"","description":"Large pothole","categoryId":%d,"latitude":16.84,"longitude":96.17}
                """.formatted(categoryId)).getBytes());

        mockMvc.perform(multipart("/api/reports").file(data)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void createReport_withInvalidLatitude_returns400WithFieldError() throws Exception {
        User citizen = persistApprovedCitizen("citizen6@example.com");
        String token = jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);
        long categoryId = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow().getId();

        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", ("""
                {"title":"Pothole","description":"Large pothole","categoryId":%d,"latitude":95.0,"longitude":96.17}
                """.formatted(categoryId)).getBytes());

        mockMvc.perform(multipart("/api/reports").file(data)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.latitude").exists());
    }

    @Test
    void createReport_withInvalidLongitude_returns400WithFieldError() throws Exception {
        User citizen = persistApprovedCitizen("citizen7@example.com");
        String token = jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);
        long categoryId = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow().getId();

        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", ("""
                {"title":"Pothole","description":"Large pothole","categoryId":%d,"latitude":16.84,"longitude":185.0}
                """.formatted(categoryId)).getBytes());

        mockMvc.perform(multipart("/api/reports").file(data)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.longitude").exists());
    }

    @Test
    void createReport_withoutAuthentication_returns401() throws Exception {
        long categoryId = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow().getId();
        MockMultipartFile data = jsonPart(categoryId, "16.8409000", "96.1735000");

        mockMvc.perform(multipart("/api/reports").file(data))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReport_withStaffToken_returns403() throws Exception {
        String staffToken = jwtUtil.generateToken(1L, "staff@example.com", RoleName.STAFF.name(), 1L);
        long categoryId = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow().getId();
        MockMultipartFile data = jsonPart(categoryId, "16.8409000", "96.1735000");

        mockMvc.perform(multipart("/api/reports").file(data)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void assign_withAdminAndValidDepartment_returns200AndUpdatesDepartment() throws Exception {
        Department roads = departmentRepository.findByName("Roads").orElseThrow();
        Department water = departmentRepository.findByName("Water").orElseThrow();
        Category pothole = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
        User reporter = persistApprovedCitizen("assign-reporter@example.com");
        Report report = persistReport(reporter, pothole, roads, ReportStatus.ASSIGNED);
        String adminToken = adminToken();

        mockMvc.perform(patch("/api/reports/" + report.getId() + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + water.getId() + "}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(water.getId()));
    }

    @Test
    void assign_withStaffToken_returns403() throws Exception {
        Department roads = departmentRepository.findByName("Roads").orElseThrow();
        Category pothole = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
        User reporter = persistApprovedCitizen("assign-reporter2@example.com");
        Report report = persistReport(reporter, pothole, roads, ReportStatus.ASSIGNED);
        String staffToken = jwtUtil.generateToken(1L, "staff-assign@example.com", RoleName.STAFF.name(), roads.getId());

        mockMvc.perform(patch("/api/reports/" + report.getId() + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + roads.getId() + "}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void priority_withStaffOwningDepartment_returns200() throws Exception {
        Department roads = departmentRepository.findByName("Roads").orElseThrow();
        Category pothole = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
        User reporter = persistApprovedCitizen("priority-reporter@example.com");
        Report report = persistReport(reporter, pothole, roads, ReportStatus.ASSIGNED);
        String staffToken = jwtUtil.generateToken(1L, "staff-priority@example.com", RoleName.STAFF.name(), roads.getId());

        mockMvc.perform(patch("/api/reports/" + report.getId() + "/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"URGENT\"}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("URGENT"));
    }

    @Test
    void priority_withCitizenToken_returns403() throws Exception {
        Department roads = departmentRepository.findByName("Roads").orElseThrow();
        Category pothole = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
        User reporter = persistApprovedCitizen("priority-reporter2@example.com");
        Report report = persistReport(reporter, pothole, roads, ReportStatus.ASSIGNED);
        String citizenToken = jwtUtil.generateToken(reporter.getId(), reporter.getEmail(), RoleName.CITIZEN.name(), null);

        mockMvc.perform(patch("/api/reports/" + report.getId() + "/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"HIGH\"}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void comments_postThenGet_returnsTheCreatedComment() throws Exception {
        Department roads = departmentRepository.findByName("Roads").orElseThrow();
        Category pothole = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
        User reporter = persistApprovedCitizen("comment-reporter@example.com");
        Report report = persistReport(reporter, pothole, roads, ReportStatus.ASSIGNED);
        String staffToken = jwtUtil.generateToken(1L, "staff-comment@example.com", RoleName.STAFF.name(), roads.getId());

        mockMvc.perform(post("/api/reports/" + report.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Crew scheduled for tomorrow.\"}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Crew scheduled for tomorrow."));

        mockMvc.perform(get("/api/reports/" + report.getId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void comments_withCitizenToken_returns403() throws Exception {
        Department roads = departmentRepository.findByName("Roads").orElseThrow();
        Category pothole = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
        User reporter = persistApprovedCitizen("comment-reporter2@example.com");
        Report report = persistReport(reporter, pothole, roads, ReportStatus.ASSIGNED);
        String citizenToken = jwtUtil.generateToken(reporter.getId(), reporter.getEmail(), RoleName.CITIZEN.name(), null);

        mockMvc.perform(get("/api/reports/" + report.getId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void comments_withStaffFromAnotherDepartment_returns403() throws Exception {
        Department roads = departmentRepository.findByName("Roads").orElseThrow();
        Category pothole = categoryRepository.findByName("Pothole / Damaged Road").orElseThrow();
        User reporter = persistApprovedCitizen("comment-reporter3@example.com");
        Report report = persistReport(reporter, pothole, roads, ReportStatus.ASSIGNED);
        String otherStaffToken = jwtUtil.generateToken(2L, "staff-other-dept@example.com", RoleName.STAFF.name(),
                departmentRepository.findByName("Water").orElseThrow().getId());

        mockMvc.perform(get("/api/reports/" + report.getId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherStaffToken))
                .andExpect(status().isForbidden());
    }

    private String adminToken() {
        User admin = userRepository.findByEmail("admin@scirs.gov").orElseThrow();
        return jwtUtil.generateToken(admin.getId(), admin.getEmail(), RoleName.ADMIN.name(), null);
    }

    private Report persistReport(User reporter, Category category, Department department, ReportStatus status) {
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
        return reportRepository.save(report);
    }

    private MockMultipartFile jsonPart(long categoryId, String latitude, String longitude) {
        String json = """
                {"title":"Pothole on Main St","description":"Large pothole blocking traffic",
                 "categoryId":%d,"latitude":%s,"longitude":%s}""".formatted(categoryId, latitude, longitude);
        return new MockMultipartFile("data", "", "application/json", json.getBytes());
    }

    private User persistApprovedCitizen(String email) {
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
}
