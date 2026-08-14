package com.uit.scirs.department.mapper;

import com.uit.scirs.department.dto.CreateDepartmentDTO;
import com.uit.scirs.department.dto.DepartmentDTO;
import com.uit.scirs.department.entity.Department;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DepartmentMapper {

    public DepartmentDTO toDTO(Department entity) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setContactEmail(entity.getContactEmail());
        dto.setActive(entity.isActive());
        return dto;
    }

    public Department toEntity(CreateDepartmentDTO dto) {
        Department entity = new Department();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setContactEmail(dto.getContactEmail());
        return entity;
    }

    public List<DepartmentDTO> toDTOList(List<Department> entities) {
        return entities.stream().map(this::toDTO).toList();
    }
}
