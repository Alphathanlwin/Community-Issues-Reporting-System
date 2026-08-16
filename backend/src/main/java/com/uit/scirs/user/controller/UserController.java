package com.uit.scirs.user.controller;

import com.uit.scirs.auth.dto.UserDTO;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.user.dto.CreateStaffDTO;
import com.uit.scirs.user.dto.RejectUserDTO;
import com.uit.scirs.user.dto.UpdateUserDTO;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAll(@RequestParam(required = false) RoleName role,
                                                 @RequestParam(required = false) AccountStatus accountStatus) {
        return ResponseEntity.ok(userService.getAll(role, accountStatus));
    }

    @GetMapping("/citizens")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getCitizens() {
        return ResponseEntity.ok(userService.getCitizens());
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getStaff() {
        return ResponseEntity.ok(userService.getStaff());
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createStaff(@Valid @RequestBody CreateStaffDTO dto) {
        UserDTO created = userService.createStaff(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getPending() {
        return ResponseEntity.ok(userService.getPending());
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(userService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> reject(@PathVariable Long id, @Valid @RequestBody RejectUserDTO dto) {
        return ResponseEntity.ok(userService.reject(id, dto.getReason()));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> suspend(@PathVariable Long id) {
        return ResponseEntity.ok(userService.suspend(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<UserDTO> getById(@PathVariable Long id,
                                            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(userService.getById(id, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<UserDTO> update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateUserDTO dto,
                                           @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(userService.update(id, dto, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
