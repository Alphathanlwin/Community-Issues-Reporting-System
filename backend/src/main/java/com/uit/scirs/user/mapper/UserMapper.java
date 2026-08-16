package com.uit.scirs.user.mapper;

import com.uit.scirs.auth.dto.UserDTO;
import com.uit.scirs.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserDTO toDTO(User entity) {
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

    public List<UserDTO> toDTOList(List<User> entities) {
        return entities.stream().map(this::toDTO).toList();
    }
}
