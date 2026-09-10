package com.uit.scirs.department.service;

import com.uit.scirs.audit.entity.AuditAction;
import com.uit.scirs.audit.service.AuditService;
import com.uit.scirs.common.config.CacheConfig;
import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.department.dto.CreateDepartmentDTO;
import com.uit.scirs.department.dto.DepartmentDTO;
import com.uit.scirs.department.dto.UpdateDepartmentDTO;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.mapper.DepartmentMapper;
import com.uit.scirs.department.repository.DepartmentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final AuditService auditService;

    public DepartmentService(DepartmentRepository departmentRepository,
                             DepartmentMapper departmentMapper,
                             AuditService auditService) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
        this.auditService = auditService;
    }

    @Cacheable(CacheConfig.DEPARTMENTS)
    public List<DepartmentDTO> getAll() {
        return departmentMapper.toDTOList(departmentRepository.findAll());
    }

    public DepartmentDTO getById(Long id) {
        return departmentMapper.toDTO(findEntity(id));
    }

    @CacheEvict(value = CacheConfig.DEPARTMENTS, allEntries = true)
    @Transactional
    public DepartmentDTO create(CreateDepartmentDTO dto) {
        if (departmentRepository.findByName(dto.getName()).isPresent()) {
            throw new DuplicateResourceException("A department named '" + dto.getName() + "' already exists");
        }

        Department saved = departmentRepository.save(departmentMapper.toEntity(dto));
        auditService.record(AuditAction.DEPARTMENT_CREATED, "DEPARTMENT", saved.getId(),
                saved.getName(), "Department created");
        return departmentMapper.toDTO(saved);
    }

    @CacheEvict(value = CacheConfig.DEPARTMENTS, allEntries = true)
    @Transactional
    public DepartmentDTO update(Long id, UpdateDepartmentDTO dto) {
        Department department = findEntity(id);

        departmentRepository.findByName(dto.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("A department named '" + dto.getName() + "' already exists");
                });

        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        department.setContactEmail(dto.getContactEmail());

        Department saved = departmentRepository.save(department);
        auditService.record(AuditAction.DEPARTMENT_UPDATED, "DEPARTMENT", saved.getId(),
                saved.getName(), "Department details updated");
        return departmentMapper.toDTO(saved);
    }

    @CacheEvict(value = CacheConfig.DEPARTMENTS, allEntries = true)
    @Transactional
    public void delete(Long id) {
        Department department = findEntity(id);
        department.setActive(false);
        departmentRepository.save(department);
        auditService.record(AuditAction.DEPARTMENT_DEACTIVATED, "DEPARTMENT", department.getId(),
                department.getName(), "Department deactivated");
    }

    private Department findEntity(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }
}
