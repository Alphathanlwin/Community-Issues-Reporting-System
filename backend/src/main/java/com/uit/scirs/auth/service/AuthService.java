package com.uit.scirs.auth.service;

import com.uit.scirs.auth.dto.AuthResponseDTO;
import com.uit.scirs.auth.dto.LoginRequestDTO;
import com.uit.scirs.auth.dto.UserDTO;
import com.uit.scirs.auth.mapper.AuthMapper;
import com.uit.scirs.common.exception.AccountNotApprovedException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.security.JwtUtil;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil,
                        AuthMapper authMapper) {
        this.userRepository = userRepository;
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
