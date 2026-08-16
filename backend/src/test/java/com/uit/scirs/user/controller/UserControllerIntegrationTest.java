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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void createStaff_asAdmin_returns201WithApprovedAccount() throws Exception {
        Long departmentId = departmentRepository.findByName("Roads").orElseThrow().getId();
        String adminToken = adminToken();

        mockMvc.perform(post("/api/users/staff")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(("""
                                {"fullName":"New Staff","email":"newstaff@scirs.gov","phone":"+959123456789",
                                 "password":"securePass123","departmentId":%d}
                                """).formatted(departmentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountStatus").value("APPROVED"))
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    void createStaff_withUnknownDepartment_returns404() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(post("/api/users/staff")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {"fullName":"New Staff","email":"newstaff2@scirs.gov","phone":"+959123456780",
                                 "password":"securePass123","departmentId":999999}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void createStaff_asCitizen_returns403() throws Exception {
        User citizen = persistCitizen("citizen-users-test@example.com");
        String citizenToken = jwtUtil.generateToken(citizen.getId(), citizen.getEmail(), RoleName.CITIZEN.name(), null);
        Long departmentId = departmentRepository.findByName("Roads").orElseThrow().getId();

        mockMvc.perform(post("/api/users/staff")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + citizenToken)
                        .contentType("application/json")
                        .content(("""
                                {"fullName":"New Staff","email":"blocked@scirs.gov","phone":"+959123456781",
                                 "password":"securePass123","departmentId":%d}
                                """).formatted(departmentId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_asAdmin_flipsAccountStatusToApproved() throws Exception {
        User citizen = persistCitizen("citizen-approve-test@example.com");
        citizen.setAccountStatus(AccountStatus.PENDING);
        userRepository.save(citizen);
        String adminToken = adminToken();

        mockMvc.perform(patch("/api/users/" + citizen.getId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("APPROVED"));
    }

    @Test
    void approve_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(patch("/api/users/1/approve"))
                .andExpect(status().isUnauthorized());
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

    private String adminToken() {
        User admin = userRepository.findByEmail("admin@scirs.gov").orElseThrow();
        return jwtUtil.generateToken(admin.getId(), admin.getEmail(), RoleName.ADMIN.name(), null);
    }
}
