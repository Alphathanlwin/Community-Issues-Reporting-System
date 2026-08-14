package com.uit.scirs.department.service;

import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.department.dto.CreateDepartmentDTO;
import com.uit.scirs.department.dto.DepartmentDTO;
import com.uit.scirs.department.dto.UpdateDepartmentDTO;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.mapper.DepartmentMapper;
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
class DepartmentServiceTest {

    @Mock DepartmentRepository departmentRepository;
    @Mock DepartmentMapper departmentMapper;
    @InjectMocks DepartmentService departmentService;

    @Test
    void create_withUniqueName_savesDepartment() {
        CreateDepartmentDTO dto = createDto("Electricity");
        Department entity = department(null, "Electricity", true);
        Department saved = department(1L, "Electricity", true);

        when(departmentRepository.findByName("Electricity")).thenReturn(Optional.empty());
        when(departmentMapper.toEntity(dto)).thenReturn(entity);
        when(departmentRepository.save(entity)).thenReturn(saved);
        when(departmentMapper.toDTO(saved)).thenReturn(dtoFor(saved));

        DepartmentDTO result = departmentService.create(dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electricity");
    }

    @Test
    void create_withDuplicateName_throwsDuplicateResourceException() {
        CreateDepartmentDTO dto = createDto("Roads");
        when(departmentRepository.findByName("Roads")).thenReturn(Optional.of(department(1L, "Roads", true)));

        assertThatThrownBy(() -> departmentService.create(dto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    void getById_withUnknownId_throwsResourceNotFoundException() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_setsActiveFalseInsteadOfRemovingRow() {
        Department entity = department(1L, "Water", true);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(departmentRepository.save(any(Department.class))).thenAnswer(i -> i.getArgument(0));

        departmentService.delete(1L);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void update_withNameBelongingToAnotherDepartment_throwsDuplicateResourceException() {
        Department target = department(1L, "Water", true);
        Department other = department(2L, "Sanitation", true);
        UpdateDepartmentDTO dto = updateDto("Sanitation");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(target));
        when(departmentRepository.findByName("Sanitation")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> departmentService.update(1L, dto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(departmentRepository, never()).save(target);
    }

    private CreateDepartmentDTO createDto(String name) {
        CreateDepartmentDTO dto = new CreateDepartmentDTO();
        dto.setName(name);
        dto.setDescription(name + " department");
        dto.setContactEmail(name.toLowerCase() + "@scirs.gov");
        return dto;
    }

    private UpdateDepartmentDTO updateDto(String name) {
        UpdateDepartmentDTO dto = new UpdateDepartmentDTO();
        dto.setName(name);
        dto.setDescription(name + " department");
        dto.setContactEmail(name.toLowerCase() + "@scirs.gov");
        return dto;
    }

    private Department department(Long id, String name, boolean active) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setActive(active);
        return department;
    }

    private DepartmentDTO dtoFor(Department entity) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setActive(entity.isActive());
        return dto;
    }
}
