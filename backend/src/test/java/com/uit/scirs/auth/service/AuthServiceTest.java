package com.uit.scirs.auth.service;

import com.uit.scirs.auth.dto.AuthResponseDTO;
import com.uit.scirs.auth.dto.CitizenRegisterDTO;
import com.uit.scirs.auth.dto.LoginRequestDTO;
import com.uit.scirs.auth.dto.RegisterResponseDTO;
import com.uit.scirs.auth.mapper.AuthMapper;
import com.uit.scirs.common.exception.AccountNotApprovedException;
import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.common.security.JwtUtil;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.Role;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.RoleRepository;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthMapper authMapper;
    @InjectMocks AuthService authService;

    @Test
    void login_withValidCredentials_returnsAuthResponseWithToken() {
        Role citizenRole = new Role(RoleName.CITIZEN, "Citizen");
        User user = approvedUser(citizenRole);
        LoginRequestDTO dto = loginRequest("citizen@example.com", "correctPass1");

        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPass1", user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(user.getId(), user.getEmail(), "CITIZEN", null)).thenReturn("jwt-token");
        when(authMapper.toAuthResponse(user, "jwt-token"))
                .thenReturn(new AuthResponseDTO("jwt-token", user.getId(), user.getFullName(), user.getEmail(),
                        "CITIZEN", null, "APPROVED"));

        AuthResponseDTO result = authService.login(dto);

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getRole()).isEqualTo("CITIZEN");
    }

    @Test
    void login_withWrongPassword_throwsBadCredentialsException() {
        Role citizenRole = new Role(RoleName.CITIZEN, "Citizen");
        User user = approvedUser(citizenRole);
        LoginRequestDTO dto = loginRequest("citizen@example.com", "wrongPass");

        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withUnknownEmail_throwsBadCredentialsException() {
        LoginRequestDTO dto = loginRequest("nobody@example.com", "anyPass1");
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withPendingAccount_throwsAccountNotApprovedException() {
        Role citizenRole = new Role(RoleName.CITIZEN, "Citizen");
        User user = approvedUser(citizenRole);
        user.setAccountStatus(AccountStatus.PENDING);
        LoginRequestDTO dto = loginRequest("citizen@example.com", "correctPass1");

        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPass1", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(AccountNotApprovedException.class);
    }

    @Test
    void register_withValidData_createsPendingCitizenAccount() {
        Role citizenRole = new Role(RoleName.CITIZEN, "Citizen");
        CitizenRegisterDTO dto = registerRequest();

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(dto.getPhone())).thenReturn(false);
        when(userRepository.existsByNrcNumber(dto.getNrcNumber())).thenReturn(false);
        when(roleRepository.findByName(RoleName.CITIZEN)).thenReturn(Optional.of(citizenRole));
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authMapper.toRegisterResponse(any(User.class), anyString()))
                .thenReturn(new RegisterResponseDTO(1L, dto.getFullName(), dto.getEmail(), "CITIZEN", "PENDING",
                        "Your account is pending admin approval."));

        RegisterResponseDTO result = authService.register(dto);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getRole()).isEqualTo(citizenRole);
        assertThat(savedUser.getValue().getAccountStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(result.getAccountStatus()).isEqualTo("PENDING");
        assertThat(result.getRole()).isEqualTo("CITIZEN");
    }

    @Test
    void register_withDuplicateEmail_throwsDuplicateResourceException() {
        CitizenRegisterDTO dto = registerRequest();
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_withDuplicateNrc_throwsDuplicateResourceException() {
        CitizenRegisterDTO dto = registerRequest();
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(dto.getPhone())).thenReturn(false);
        when(userRepository.existsByNrcNumber(dto.getNrcNumber())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    private User approvedUser(Role role) {
        User user = new User();
        user.setId(1L);
        user.setFullName("Test Citizen");
        user.setEmail("citizen@example.com");
        user.setPasswordHash("bcrypt-hash");
        user.setRole(role);
        user.setAccountStatus(AccountStatus.APPROVED);
        return user;
    }

    private LoginRequestDTO loginRequest(String email, String password) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    private CitizenRegisterDTO registerRequest() {
        CitizenRegisterDTO dto = new CitizenRegisterDTO();
        dto.setFullName("Aung Aung");
        dto.setEmail("aung@example.com");
        dto.setPhone("+959123456789");
        dto.setPassword("securePass123");
        dto.setDateOfBirth(LocalDate.of(2002, 5, 14));
        dto.setNrcNumber("12/ABC(N)123456");
        return dto;
    }
}
