package com.uit.scirs.auth.mapper;

import com.uit.scirs.auth.dto.AuthResponseDTO;
import com.uit.scirs.auth.dto.RegisterResponseDTO;
import com.uit.scirs.auth.dto.UserDTO;
import com.uit.scirs.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public UserDTO toUserDTO(User entity) {
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setFullName(entity.getFullName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setRole(entity.getRole().getName().name());
        dto.setAccountStatus(entity.getAccountStatus().name());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setNrcNumber(entity.getNrcNumber());
        dto.setDepartmentId(entity.getDepartmentId());
        dto.setScorePoints(entity.getScorePoints());
        dto.setProfileImageUrl(entity.getProfileImageUrl());
        dto.setActive(entity.isActive());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public AuthResponseDTO toAuthResponse(User entity, String token) {
        return new AuthResponseDTO(
                token,
                entity.getId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getRole().getName().name(),
                entity.getDepartmentId(),
                entity.getAccountStatus().name());
    }

    public RegisterResponseDTO toRegisterResponse(User entity, String message) {
        return new RegisterResponseDTO(
                entity.getId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getRole().getName().name(),
                entity.getAccountStatus().name(),
                message);
    }
}
