package com.uit.scirs.category.service;

import com.uit.scirs.category.dto.CategoryDTO;
import com.uit.scirs.category.dto.CreateCategoryDTO;
import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.mapper.CategoryMapper;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock CategoryMapper categoryMapper;
    @InjectMocks CategoryService categoryService;

    @Test
    void create_withActiveDepartment_savesCategory() {
        Department roads = department(2L, "Roads", true);
        CreateCategoryDTO dto = createDto("Pothole / Damaged Road", 2L);

        when(categoryRepository.findByName(dto.getName())).thenReturn(Optional.empty());
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(roads));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> {
            Category saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(categoryMapper.toDTO(any(Category.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        CategoryDTO result = categoryService.create(dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDepartmentId()).isEqualTo(2L);
    }

    @Test
    void create_withInactiveDepartment_throwsBusinessRuleException() {
        Department roads = department(2L, "Roads", false);
        CreateCategoryDTO dto = createDto("Pothole / Damaged Road", 2L);

        when(categoryRepository.findByName(dto.getName())).thenReturn(Optional.empty());
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(roads));

        assertThatThrownBy(() -> categoryService.create(dto))
                .isInstanceOf(BusinessRuleException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void create_withUnknownDepartment_throwsResourceNotFoundException() {
        CreateCategoryDTO dto = createDto("Pothole / Damaged Road", 99L);

        when(categoryRepository.findByName(dto.getName())).thenReturn(Optional.empty());
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void create_withDuplicateName_throwsDuplicateResourceException() {
        CreateCategoryDTO dto = createDto("Pothole / Damaged Road", 2L);
        when(categoryRepository.findByName(dto.getName()))
                .thenReturn(Optional.of(category(1L, "Pothole / Damaged Road", department(2L, "Roads", true))));

        assertThatThrownBy(() -> categoryService.create(dto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void delete_setsActiveFalseInsteadOfRemovingRow() {
        Category entity = category(1L, "Garbage / Sanitation", department(4L, "Sanitation", true));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        categoryService.delete(1L);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void getById_withUnknownId_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CreateCategoryDTO createDto(String name, Long departmentId) {
        CreateCategoryDTO dto = new CreateCategoryDTO();
        dto.setName(name);
        dto.setDescription(name);
        dto.setDepartmentId(departmentId);
        dto.setIcon("road");
        dto.setColorHex("#F97316");
        return dto;
    }

    private Department department(Long id, String name, boolean active) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setActive(active);
        return department;
    }

    private Category category(Long id, String name, Department department) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDepartment(department);
        category.setActive(true);
        return category;
    }

    private CategoryDTO dtoFor(Category entity) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartmentName(entity.getDepartment().getName());
        }
        dto.setActive(entity.isActive());
        return dto;
    }
}
