package com.uit.scirs.user.controller;

import com.uit.scirs.common.security.JwtUtil;
import com.uit.scirs.department.repository.DepartmentRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired DepartmentRepository departmentRepository;

    @Test
    void createStaff_withAdminTokenAndActiveDepartment_returns201() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);
        long roadsId = departmentRepository.findByName("Roads").orElseThrow().getId();

        mockMvc.perform(post("/api/users/staff")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Road Crew Lead","email":"roadcrew@scirs.gov",
                                 "phone":"+959123456780","password":"securePass123","departmentId":%d}
                                """.formatted(roadsId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.accountStatus").value("APPROVED"))
                .andExpect(jsonPath("$.departmentId").value(roadsId));
    }

    @Test
    void createStaff_withInactiveDepartment_returns400() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);
        long buildingsId = departmentRepository.findByName("Buildings").orElseThrow().getId();

        mockMvc.perform(delete("/api/departments/" + buildingsId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/users/staff")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Ghost Staff","email":"ghoststaff@scirs.gov",
                                 "password":"securePass123","departmentId":%d}
                                """.formatted(buildingsId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createStaff_withUnknownDepartment_returns404() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);

        mockMvc.perform(post("/api/users/staff")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Mystery Staff","email":"mysterystaff@scirs.gov",
                                 "password":"securePass123","departmentId":999999}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void createStaff_withCitizenToken_returns403() throws Exception {
        String citizenToken = jwtUtil.generateToken(2L, "citizen@example.com", RoleName.CITIZEN.name(), null);
        long roadsId = departmentRepository.findByName("Roads").orElseThrow().getId();

        mockMvc.perform(post("/api/users/staff")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Sneaky Staff","email":"sneaky@scirs.gov",
                                 "password":"securePass123","departmentId":%d}
                                """.formatted(roadsId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPending_withAdminToken_returnsOnlyPendingCitizens() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);
        persistCitizen("pending1@example.com", AccountStatus.PENDING);
        persistCitizen("approved1@example.com", AccountStatus.APPROVED);

        mockMvc.perform(get("/api/users/pending")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'pending1@example.com')]").exists())
                .andExpect(jsonPath("$[?(@.email == 'approved1@example.com')]").doesNotExist());
    }

    @Test
    void getPending_withCitizenToken_returns403() throws Exception {
        String citizenToken = jwtUtil.generateToken(2L, "citizen@example.com", RoleName.CITIZEN.name(), null);

        mockMvc.perform(get("/api/users/pending")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_pendingCitizen_returns200WithApprovedStatus() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);
        User pending = persistCitizen("toapprove@example.com", AccountStatus.PENDING);

        mockMvc.perform(patch("/api/users/" + pending.getId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("APPROVED"));
    }

    @Test
    void reject_pendingCitizenWithoutReason_returns400() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);
        User pending = persistCitizen("toreject@example.com", AccountStatus.PENDING);

        mockMvc.perform(patch("/api/users/" + pending.getId() + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
    }

    @Test
    void reject_pendingCitizenWithReason_returns200WithRejectedStatus() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);
        User pending = persistCitizen("toreject2@example.com", AccountStatus.PENDING);

        mockMvc.perform(patch("/api/users/" + pending.getId() + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Invalid NRC document\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("REJECTED"));
    }

    @Test
    void suspend_approvedCitizen_returns200WithSuspendedStatus() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);
        User approved = persistCitizen("tosuspend@example.com", AccountStatus.APPROVED);

        mockMvc.perform(patch("/api/users/" + approved.getId() + "/suspend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("SUSPENDED"));
    }

    @Test
    void getById_selfAccess_returns200() throws Exception {
        User citizen = persistCitizen("self@example.com", AccountStatus.APPROVED);
        String token = jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);

        mockMvc.perform(get("/api/users/" + citizen.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("self@example.com"));
    }

    @Test
    void getById_anotherCitizensRecord_returns403() throws Exception {
        User citizen = persistCitizen("owner@example.com", AccountStatus.APPROVED);
        User intruder = persistCitizen("intruder@example.com", AccountStatus.APPROVED);
        String intruderToken = jwtUtil.generateToken(intruder.getId(), intruder.getEmail(), RoleName.CITIZEN.name(), null);

        mockMvc.perform(get("/api/users/" + citizen.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_selfProfile_returns200WithUpdatedFields() throws Exception {
        User citizen = persistCitizen("update@example.com", AccountStatus.APPROVED);
        String token = jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);

        mockMvc.perform(put("/api/users/" + citizen.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Updated Citizen Name","phone":"+959999999999"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Citizen Name"));
    }

    @Test
    void delete_withAdminToken_returns204AndSoftDeletes() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin@scirs.gov", RoleName.ADMIN.name(), null);
        User citizen = persistCitizen("todelete@example.com", AccountStatus.APPROVED);

        mockMvc.perform(delete("/api/users/" + citizen.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/" + citizen.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(citizen.getId()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void delete_withCitizenToken_returns403() throws Exception {
        String citizenToken = jwtUtil.generateToken(2L, "citizen@example.com", RoleName.CITIZEN.name(), null);
        User citizen = persistCitizen("keepme@example.com", AccountStatus.APPROVED);

        mockMvc.perform(delete("/api/users/" + citizen.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
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
}
