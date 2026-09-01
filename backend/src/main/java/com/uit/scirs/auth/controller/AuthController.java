package com.uit.scirs.auth.controller;

import com.uit.scirs.auth.dto.AuthResponseDTO;
import com.uit.scirs.auth.dto.CitizenRegisterDTO;
import com.uit.scirs.auth.dto.LoginRequestDTO;
import com.uit.scirs.auth.dto.RegisterResponseDTO;
import com.uit.scirs.auth.dto.UserDTO;
import com.uit.scirs.auth.service.AuthService;
import com.uit.scirs.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Login (all roles), citizen self-registration, current-user profile")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody CitizenRegisterDTO dto) {
        RegisterResponseDTO created = authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(authService.me(currentUser.getId()));
    }
}
