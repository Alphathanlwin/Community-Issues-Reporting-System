package com.uit.scirs.auth.service;

import com.uit.scirs.auth.dto.AuthResponseDTO;
import com.uit.scirs.auth.dto.CitizenRegisterDTO;
import com.uit.scirs.auth.dto.LoginRequestDTO;
import com.uit.scirs.auth.dto.RegisterResponseDTO;
import com.uit.scirs.auth.dto.UserDTO;
import com.uit.scirs.auth.mapper.AuthMapper;
import com.uit.scirs.common.exception.AccountNotApprovedException;
import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.security.JwtUtil;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.Role;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.RoleRepository;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;

    public AuthService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil,
                        AuthMapper authMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authMapper = authMapper;
    }

    public AuthResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getAccountStatus() != AccountStatus.APPROVED) {
            throw new AccountNotApprovedException(accountStatusMessage(user.getAccountStatus()));
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(),
                user.getRole().getName().name(), user.getDepartmentId());

        return authMapper.toAuthResponse(user, token);
    }

    @Transactional
    public RegisterResponseDTO register(CitizenRegisterDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }
        if (dto.getPhone() != null && userRepository.existsByPhone(dto.getPhone())) {
            throw new DuplicateResourceException("An account with this phone number already exists.");
        }
        if (userRepository.existsByNrcNumber(dto.getNrcNumber())) {
            throw new DuplicateResourceException("An account with this NRC number already exists.");
        }

        Role citizenRole = roleRepository.findByName(RoleName.CITIZEN)
                .orElseThrow(() -> new ResourceNotFoundException("CITIZEN role is not seeded"));

        User citizen = new User();
        citizen.setFullName(dto.getFullName());
        citizen.setEmail(dto.getEmail());
        citizen.setPhone(dto.getPhone());
        citizen.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        citizen.setRole(citizenRole);
        citizen.setAccountStatus(AccountStatus.PENDING);
        citizen.setDateOfBirth(dto.getDateOfBirth());
        citizen.setNrcNumber(dto.getNrcNumber());

        User saved = userRepository.save(citizen);

        return authMapper.toRegisterResponse(saved, "Your account is pending admin approval.");
    }

    public UserDTO me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return authMapper.toUserDTO(user);
    }

    private String accountStatusMessage(AccountStatus status) {
        return switch (status) {
            case PENDING -> "Your account is awaiting admin approval.";
            case REJECTED -> "Your account registration was rejected.";
            case SUSPENDED -> "Your account has been suspended.";
            case APPROVED -> throw new IllegalStateException("Unreachable: account is approved");
        };
    }
}
