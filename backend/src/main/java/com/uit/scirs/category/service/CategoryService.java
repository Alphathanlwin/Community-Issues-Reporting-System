package com.uit.scirs.category.service;

import com.uit.scirs.category.dto.CategoryDTO;
import com.uit.scirs.category.dto.CreateCategoryDTO;
import com.uit.scirs.category.dto.UpdateCategoryDTO;
import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.mapper.CategoryMapper;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.common.config.CacheConfig;
import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.repository.DepartmentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository,
                            DepartmentRepository departmentRepository,
                            CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.categoryMapper = categoryMapper;
    }

    @Cacheable(CacheConfig.CATEGORIES)
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAll() {
        return categoryMapper.toDTOList(categoryRepository.findAll());
    }

    @Transactional(readOnly = true)
    public CategoryDTO getById(Long id) {
        return categoryMapper.toDTO(findEntity(id));
    }

    @CacheEvict(value = CacheConfig.CATEGORIES, allEntries = true)
    @Transactional
    public CategoryDTO create(CreateCategoryDTO dto) {
        if (categoryRepository.findByName(dto.getName()).isPresent()) {
            throw new DuplicateResourceException("A category named '" + dto.getName() + "' already exists");
        }

        Department department = requireActiveDepartment(dto.getDepartmentId());

        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setDepartment(department);
        category.setIcon(dto.getIcon());
        category.setColorHex(dto.getColorHex());

        return categoryMapper.toDTO(categoryRepository.save(category));
    }

    @CacheEvict(value = CacheConfig.CATEGORIES, allEntries = true)
    @Transactional
    public CategoryDTO update(Long id, UpdateCategoryDTO dto) {
        Category category = findEntity(id);

        categoryRepository.findByName(dto.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("A category named '" + dto.getName() + "' already exists");
                });

        Department department = requireActiveDepartment(dto.getDepartmentId());

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setDepartment(department);
        category.setIcon(dto.getIcon());
        category.setColorHex(dto.getColorHex());

        return categoryMapper.toDTO(categoryRepository.save(category));
    }

    @CacheEvict(value = CacheConfig.CATEGORIES, allEntries = true)
    @Transactional
    public void delete(Long id) {
        Category category = findEntity(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private Department requireActiveDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        if (!department.isActive()) {
            throw new BusinessRuleException("Category must reference an active department");
        }

        return department;
    }

    private Category findEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }
}
