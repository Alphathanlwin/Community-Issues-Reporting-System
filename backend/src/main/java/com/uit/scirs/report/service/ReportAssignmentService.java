package com.uit.scirs.report.service;

import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.repository.DepartmentRepository;
import com.uit.scirs.report.dto.AssignReportDTO;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.mapper.ReportMapper;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * ADMIN-only manual reassignment: change a report's department and/or
 * assigned staff member without going through the approve/status transition
 * matrix (see architecture.md § Key Workflow — Reassignment).
 */
@Service
public class ReportAssignmentService {

    private static final Set<ReportStatus> REASSIGNABLE_STATUSES =
            EnumSet.of(ReportStatus.ASSIGNED, ReportStatus.IN_PROGRESS, ReportStatus.RESOLVED);

    private final ReportRepository reportRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;
    private final StatusHistoryService statusHistoryService;

    public ReportAssignmentService(ReportRepository reportRepository,
                                    DepartmentRepository departmentRepository,
                                    UserRepository userRepository,
                                    ReportMapper reportMapper,
                                    StatusHistoryService statusHistoryService) {
        this.reportRepository = reportRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.reportMapper = reportMapper;
        this.statusHistoryService = statusHistoryService;
    }

    @Transactional
    public ReportDTO assign(Long reportId, AssignReportDTO dto, CurrentUser admin) {
        if (dto.getDepartmentId() == null && dto.getStaffId() == null) {
            throw new BusinessRuleException("Provide a departmentId and/or a staffId to reassign.");
        }

        Report report = findReport(reportId);
        if (!REASSIGNABLE_STATUSES.contains(report.getStatus())) {
            throw new BusinessRuleException("Cannot reassign a report in status " + report.getStatus());
        }

        List<String> remarks = new ArrayList<>();
        Department targetDepartment = report.getDepartment();

        if (dto.getDepartmentId() != null) {
            targetDepartment = requireActiveDepartment(dto.getDepartmentId());
            report.setDepartment(targetDepartment);
            remarks.add("Reassigned to " + targetDepartment.getName() + " department.");
        }

        if (dto.getStaffId() != null) {
            User staff = requireStaffInDepartment(dto.getStaffId(), targetDepartment);
            report.setAssignedStaff(staff);
            remarks.add("Assigned to " + staff.getFullName() + ".");
        }

        // Resolved last: only needed to attribute the audit-log row below, so
        // every request-validation failure above must surface its own
        // exception rather than a misleading "acting user not found".
        User adminUser = findUser(admin.getId());
        Report saved = reportRepository.save(report);
        statusHistoryService.record(saved, saved.getStatus(), saved.getStatus(), adminUser, String.join(" ", remarks));

        return reportMapper.toDTO(saved);
    }

    private User requireStaffInDepartment(Long staffId, Department department) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + staffId));

        if (staff.getRole() == null || staff.getRole().getName() != RoleName.STAFF) {
            throw new BusinessRuleException("Only staff members can be assigned to a report.");
        }

        if (department == null || !department.getId().equals(staff.getDepartmentId())) {
            throw new BusinessRuleException("Staff member does not belong to the target department.");
        }

        return staff;
    }

    private Department requireActiveDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        if (!department.isActive()) {
            throw new BusinessRuleException("Cannot reassign to an inactive department.");
        }

        return department;
    }

    private Report findReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
