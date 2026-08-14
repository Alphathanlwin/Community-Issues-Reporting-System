package com.uit.scirs.category.mapper;

import com.uit.scirs.category.dto.CategoryDTO;
import com.uit.scirs.category.entity.Category;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryMapper {

    public CategoryDTO toDTO(Category entity) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartmentName(entity.getDepartment().getName());
        }
        dto.setIcon(entity.getIcon());
        dto.setColorHex(entity.getColorHex());
        dto.setActive(entity.isActive());
        return dto;
    }

    public List<CategoryDTO> toDTOList(List<Category> entities) {
        return entities.stream().map(this::toDTO).toList();
    }
}
