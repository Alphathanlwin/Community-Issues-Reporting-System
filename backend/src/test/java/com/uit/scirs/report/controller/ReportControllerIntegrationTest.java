package com.uit.scirs.report.controller;

import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.common.security.JwtUtil;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
