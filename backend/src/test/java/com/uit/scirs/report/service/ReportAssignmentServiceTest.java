package com.uit.scirs.report.service;

import com.uit.scirs.category.entity.Category;
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
import com.uit.scirs.user.entity.Role;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
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
class ReportAssignmentServiceTest {

    @Mock ReportRepository reportRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock UserRepository userRepository;
    @Mock ReportMapper reportMapper;
    @Mock StatusHistoryService statusHistoryService;
    @InjectMocks ReportAssignmentService assignmentService;

    @Test
    void assign_withDepartmentOnly_reassignsDepartmentAndWritesSameStatusHistory() {
        Department roads = department(2L, "Roads");
        Department water = department(3L, "Water");
        Report report = report(10L, ReportStatus.ASSIGNED, roads);
        User admin = user(99L, RoleName.ADMIN, null);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(water));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
        when(reportMapper.toDTO(any(Report.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        AssignReportDTO dto = new AssignReportDTO();
        dto.setDepartmentId(3L);

        ReportDTO result = assignmentService.assign(10L, dto, currentUser(99L, RoleName.ADMIN, null));

        assertThat(result.getDepartmentId()).isEqualTo(3L);
        verify(statusHistoryService).record(report, ReportStatus.ASSIGNED, ReportStatus.ASSIGNED, admin,
                "Reassigned to Water department.");
    }

    @Test
    void assign_withStaffBelongingToCurrentDepartment_assignsStaff() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, ReportStatus.ASSIGNED, roads);
        User admin = user(99L, RoleName.ADMIN, null);
        User staff = user(50L, RoleName.STAFF, 2L);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(50L)).thenReturn(Optional.of(staff));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
        when(reportMapper.toDTO(any(Report.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        AssignReportDTO dto = new AssignReportDTO();
        dto.setStaffId(50L);

        assignmentService.assign(10L, dto, currentUser(99L, RoleName.ADMIN, null));

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getAssignedStaff()).isEqualTo(staff);
    }

    @Test
    void assign_withStaffFromAnotherDepartment_throwsBusinessRuleException() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, ReportStatus.ASSIGNED, roads);
        User staff = user(50L, RoleName.STAFF, 3L);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(50L)).thenReturn(Optional.of(staff));

        AssignReportDTO dto = new AssignReportDTO();
        dto.setStaffId(50L);

        assertThatThrownBy(() -> assignmentService.assign(10L, dto, currentUser(99L, RoleName.ADMIN, null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void assign_withNonStaffUserAsStaffId_throwsBusinessRuleException() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, ReportStatus.ASSIGNED, roads);
        User citizen = user(7L, RoleName.CITIZEN, null);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));

        AssignReportDTO dto = new AssignReportDTO();
        dto.setStaffId(7L);

        assertThatThrownBy(() -> assignmentService.assign(10L, dto, currentUser(99L, RoleName.ADMIN, null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void assign_withNeitherDepartmentNorStaff_throwsBusinessRuleException() {
        AssignReportDTO dto = new AssignReportDTO();

        assertThatThrownBy(() -> assignmentService.assign(10L, dto, currentUser(99L, RoleName.ADMIN, null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(reportRepository, never()).findById(any());
    }

    @Test
    void assign_whenReportIsTerminal_throwsBusinessRuleException() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, ReportStatus.CLOSED, roads);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        AssignReportDTO dto = new AssignReportDTO();
        dto.setDepartmentId(3L);

        assertThatThrownBy(() -> assignmentService.assign(10L, dto, currentUser(99L, RoleName.ADMIN, null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void assign_withInactiveDepartment_throwsBusinessRuleException() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, ReportStatus.ASSIGNED, roads);
        Department inactive = department(4L, "Buildings");
        inactive.setActive(false);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(departmentRepository.findById(4L)).thenReturn(Optional.of(inactive));

        AssignReportDTO dto = new AssignReportDTO();
        dto.setDepartmentId(4L);

        assertThatThrownBy(() -> assignmentService.assign(10L, dto, currentUser(99L, RoleName.ADMIN, null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void assign_withUnknownDepartment_throwsResourceNotFoundException() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, ReportStatus.ASSIGNED, roads);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        AssignReportDTO dto = new AssignReportDTO();
        dto.setDepartmentId(999L);

        assertThatThrownBy(() -> assignmentService.assign(10L, dto, currentUser(99L, RoleName.ADMIN, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Department department(Long id, String name) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setActive(true);
        return department;
    }

    private Category category(Long id, String name, Department department) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDepartment(department);
        return category;
    }

    private Report report(Long id, ReportStatus status, Department department) {
        Report report = new Report();
        report.setId(id);
        report.setStatus(status);
        report.setDepartment(department);
        report.setCategory(category(1L, "Pothole", department));
        report.setReporter(user(7L, RoleName.CITIZEN, null));
        return report;
    }

    private User user(Long id, RoleName roleName, Long departmentId) {
        User user = new User();
        user.setId(id);
        user.setFullName("Test User " + id);
        user.setDepartmentId(departmentId);
        Role role = new Role(roleName, roleName.name());
        user.setRole(role);
        return user;
    }

    private CurrentUser currentUser(Long id, RoleName role, Long departmentId) {
        return new CurrentUser(id, "user@example.com", role, departmentId);
    }

    private ReportDTO dtoFor(Report entity) {
        ReportDTO dto = new ReportDTO();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus().name());
        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
        }
        return dto;
    }
}
