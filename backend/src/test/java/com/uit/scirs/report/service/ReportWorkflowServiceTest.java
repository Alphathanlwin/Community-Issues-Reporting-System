package com.uit.scirs.report.service;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.InvalidStatusTransitionException;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.notification.service.NotificationService;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.dto.UpdateReportStatusDTO;
import com.uit.scirs.report.entity.ImageType;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportImage;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.mapper.ReportMapper;
import com.uit.scirs.report.repository.ReportImageRepository;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.score.entity.PointReason;
import com.uit.scirs.score.service.ScoreService;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportWorkflowServiceTest {

    @Mock ReportRepository reportRepository;
    @Mock ReportImageRepository reportImageRepository;
    @Mock UserRepository userRepository;
    @Mock ReportMapper reportMapper;
    @Mock StatusHistoryService statusHistoryService;
    @Mock ScoreService scoreService;
    @Mock NotificationService notificationService;
    @InjectMocks ReportWorkflowService workflowService;

    @Test
    void approve_setsStatusAssignedAndRoutesToCategoryDepartment() {
        Department roads = department(2L, "Roads");
        Category pothole = category(1L, "Pothole / Damaged Road", roads);
        Report report = report(10L, ReportStatus.PENDING_APPROVAL, pothole);
        User admin = user(99L, RoleName.ADMIN, null);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
        when(reportMapper.toDTO(any(Report.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        ReportDTO result = workflowService.approve(10L, currentUser(99L, RoleName.ADMIN, null));

        assertThat(result.getStatus()).isEqualTo("ASSIGNED");
        assertThat(result.getDepartmentId()).isEqualTo(2L);
        verify(scoreService).award(report.getReporter(), PointReason.REPORT_APPROVED, report);
        verify(notificationService).notifyStatusChange(report);
    }

    @Test
    void approve_whenNotPendingApproval_throwsInvalidStatusTransitionException() {
        Report report = report(10L, ReportStatus.ASSIGNED, category(1L, "Pothole", department(2L, "Roads")));
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> workflowService.approve(10L, currentUser(99L, RoleName.ADMIN, null)))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(scoreService, never()).award(any(), any(), any());
    }

    @Test
    void reject_requiresReasonAndAwardsNegativeFive() {
        Report report = report(10L, ReportStatus.PENDING_APPROVAL, category(1L, "Pothole", department(2L, "Roads")));
        User admin = user(99L, RoleName.ADMIN, null);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
        when(reportMapper.toDTO(any(Report.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        ReportDTO result = workflowService.reject(10L, "Duplicate of another report", currentUser(99L, RoleName.ADMIN, null));

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(report.getRejectionReason()).isEqualTo("Duplicate of another report");
        verify(scoreService).award(report.getReporter(), PointReason.REPORT_REJECTED, report);
    }

    @ParameterizedTest
    @CsvSource({
            "ASSIGNED, IN_PROGRESS, true",
            "ASSIGNED, RESOLVED, true",
            "IN_PROGRESS, RESOLVED, true",
            "RESOLVED, CLOSED, true",
            "RESOLVED, IN_PROGRESS, true",
            "CLOSED, IN_PROGRESS, false",
            "ASSIGNED, CLOSED, false",
            "IN_PROGRESS, ASSIGNED, false"
    })
    void changeStatus_allowsOnlyMatrixTransitions(ReportStatus from, ReportStatus to, boolean allowed) {
        Department roads = department(2L, "Roads");
        Report report = report(10L, from, category(1L, "Pothole", roads));
        report.setDepartment(roads);
        User staff = user(50L, RoleName.STAFF, 2L);

        lenient().when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        lenient().when(userRepository.findById(50L)).thenReturn(Optional.of(staff));
        lenient().when(reportImageRepository.findByReportIdAndImageType(10L, ImageType.RESOLUTION_PHOTO))
                .thenReturn(List.of(new ReportImage()));
        lenient().when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(reportMapper.toDTO(any(Report.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        UpdateReportStatusDTO dto = new UpdateReportStatusDTO();
        dto.setStatus(to.name());

        if (allowed) {
            ReportDTO result = workflowService.changeStatus(10L, dto, currentUser(50L, RoleName.STAFF, 2L));
            assertThat(result.getStatus()).isEqualTo(to.name());
        } else {
            assertThatThrownBy(() -> workflowService.changeStatus(10L, dto, currentUser(50L, RoleName.STAFF, 2L)))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Test
    void changeStatus_toResolvedWithoutCompletionPhoto_throwsBusinessRuleException() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, ReportStatus.IN_PROGRESS, category(1L, "Pothole", roads));
        report.setDepartment(roads);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(reportImageRepository.findByReportIdAndImageType(10L, ImageType.RESOLUTION_PHOTO))
                .thenReturn(List.of());

        UpdateReportStatusDTO dto = new UpdateReportStatusDTO();
        dto.setStatus("RESOLVED");

        assertThatThrownBy(() -> workflowService.changeStatus(10L, dto, currentUser(50L, RoleName.STAFF, 2L)))
                .isInstanceOf(BusinessRuleException.class);

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void changeStatus_whenStaffFromAnotherDepartment_throwsAccessDeniedException() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, ReportStatus.ASSIGNED, category(1L, "Pothole", roads));
        report.setDepartment(roads);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        UpdateReportStatusDTO dto = new UpdateReportStatusDTO();
        dto.setStatus("IN_PROGRESS");

        assertThatThrownBy(() -> workflowService.changeStatus(10L, dto, currentUser(50L, RoleName.STAFF, 3L)))
                .isInstanceOf(AccessDeniedException.class);

        verify(reportRepository, never()).save(any(Report.class));
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

    private Report report(Long id, ReportStatus status, Category category) {
        Report report = new Report();
        report.setId(id);
        report.setStatus(status);
        report.setCategory(category);
        report.setReporter(user(7L, RoleName.CITIZEN, null));
        return report;
    }

    private User user(Long id, RoleName role, Long departmentId) {
        User user = new User();
        user.setId(id);
        user.setFullName("Test User");
        user.setDepartmentId(departmentId);
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
