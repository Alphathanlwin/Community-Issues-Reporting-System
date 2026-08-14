package com.uit.scirs.department.controller;

import com.uit.scirs.department.dto.CreateDepartmentDTO;
import com.uit.scirs.department.dto.DepartmentDTO;
import com.uit.scirs.department.dto.UpdateDepartmentDTO;
import com.uit.scirs.department.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<List<DepartmentDTO>> getAll() {
        return ResponseEntity.ok(departmentService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<DepartmentDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDTO> create(@Valid @RequestBody CreateDepartmentDTO dto) {
        DepartmentDTO created = departmentService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateDepartmentDTO dto) {
        return ResponseEntity.ok(departmentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
