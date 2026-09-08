package com.uit.scirs.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.uit.scirs.auth.dto.AuthResponseDTO;
import com.uit.scirs.auth.dto.CitizenRegisterDTO;
import com.uit.scirs.auth.dto.GoogleAuthRequestDTO;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil,
                        AuthMapper authMapper,
                        @Value("${google.oauth.client-id:}") String googleClientId) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authMapper = authMapper;
        // null (not an empty-audience verifier) when unconfigured, so
        // loginWithGoogle can tell "not set up" apart from "bad token".
        this.googleIdTokenVerifier = (googleClientId == null || googleClientId.isBlank()) ? null
                : new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(Collections.singletonList(googleClientId))
                        .build();
    }

    @Transactional(readOnly = true)
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

    /**
     * Verifies the Google ID token server-side, then either logs in the account
     * matching its (Google-verified) email, or — if no account exists yet —
     * self-registers a new CITIZEN the same way {@link #register} does: starts
     * {@code PENDING}, needs admin approval. Any existing account (any role) may
     * log in this way since Google already vouches for the email address.
     */
    @Transactional
    public AuthResponseDTO loginWithGoogle(GoogleAuthRequestDTO dto) {
        GoogleIdToken.Payload payload = verifyGoogleToken(dto.getIdToken());
        return authenticateGooglePayload(payload);
    }

    /**
     * Package-private so {@code AuthServiceTest} can exercise the account
     * lookup/creation rules directly, with a hand-built {@code Payload} —
     * the signature verification in {@link #verifyGoogleToken} needs a real
     * Google-signed token and is exercised by the integration test instead.
     */
    @Transactional
    AuthResponseDTO authenticateGooglePayload(GoogleIdToken.Payload payload) {
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new BadCredentialsException("Google account email is not verified");
        }

        User user = userRepository.findByEmail(payload.getEmail())
                .orElseGet(() -> createCitizenFromGoogle(payload));

        if (user.getAccountStatus() != AccountStatus.APPROVED) {
            throw new AccountNotApprovedException(accountStatusMessage(user.getAccountStatus()));
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(),
                user.getRole().getName().name(), user.getDepartmentId());

        return authMapper.toAuthResponse(user, token);
    }

    private User createCitizenFromGoogle(GoogleIdToken.Payload payload) {
        Role citizenRole = roleRepository.findByName(RoleName.CITIZEN)
                .orElseThrow(() -> new ResourceNotFoundException("CITIZEN role is not seeded"));

        User citizen = new User();
        citizen.setFullName(Objects.requireNonNullElse((String) payload.get("name"), payload.getEmail()));
        citizen.setEmail(payload.getEmail());
        // Google accounts never set a password. password_hash is NOT NULL, so we
        // store a random, never-disclosed hash — this account can only ever be
        // reached through Google sign-in, not the password login form.
        citizen.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        citizen.setRole(citizenRole);
        citizen.setAccountStatus(AccountStatus.PENDING);
        citizen.setProfileImageUrl((String) payload.get("picture"));

        return userRepository.save(citizen);
    }

    private GoogleIdToken.Payload verifyGoogleToken(String rawIdToken) {
        if (googleIdTokenVerifier == null) {
            throw new BadCredentialsException("Google sign-in is not configured on this server");
        }
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(rawIdToken);
            if (idToken == null) {
                throw new BadCredentialsException("Invalid Google credential");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new BadCredentialsException("Could not verify Google credential");
        }
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

    @Transactional(readOnly = true)
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
