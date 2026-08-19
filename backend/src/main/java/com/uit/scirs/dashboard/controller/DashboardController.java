package com.uit.scirs.dashboard.controller;

import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.dashboard.dto.AdminDashboardDTO;
import com.uit.scirs.dashboard.dto.CategoryVolumeDTO;
import com.uit.scirs.dashboard.dto.DepartmentPerformanceDTO;
import com.uit.scirs.dashboard.dto.StaffDashboardDTO;
import com.uit.scirs.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardDTO> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<StaffDashboardDTO> getStaffDashboard(@RequestParam(required = false) Long departmentId,
                                                                @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(dashboardService.getStaffDashboard(departmentId, currentUser));
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<List<DepartmentPerformanceDTO>> getDepartmentPerformance() {
        return ResponseEntity.ok(dashboardService.getDepartmentPerformance());
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<List<CategoryVolumeDTO>> getCategoryVolume() {
        return ResponseEntity.ok(dashboardService.getCategoryVolume());
    }
}
