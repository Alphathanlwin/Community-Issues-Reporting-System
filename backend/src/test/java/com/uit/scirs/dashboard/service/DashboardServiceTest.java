package com.uit.scirs.dashboard.service;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.dashboard.dto.AdminDashboardDTO;
import com.uit.scirs.dashboard.dto.CategoryVolumeDTO;
import com.uit.scirs.dashboard.dto.DepartmentPerformanceDTO;
import com.uit.scirs.dashboard.dto.StaffDashboardDTO;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.repository.DepartmentRepository;
import com.uit.scirs.feedback.repository.DepartmentRatingProjection;
import com.uit.scirs.feedback.repository.FeedbackRepository;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.mapper.ReportMapper;
import com.uit.scirs.report.repository.DepartmentStatusCountProjection;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.report.repository.ResolutionTimeProjection;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.mapper.UserMapper;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock ReportRepository reportRepository;
    @Mock UserRepository userRepository;
    @Mock FeedbackRepository feedbackRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ReportMapper reportMapper;
    @Mock UserMapper userMapper;
    @InjectMocks DashboardService dashboardService;

    @Test
    void getAdminDashboard_assemblesCountsAndRecentLists() {
        when(userRepository.countByAccountStatus(AccountStatus.PENDING)).thenReturn(4L);
        when(reportRepository.countByStatus(ReportStatus.PENDING_APPROVAL)).thenReturn(6L);
        User citizen = new User();
        when(userRepository.findTop10ByRoleNameOrderByCreatedAtDesc(RoleName.CITIZEN)).thenReturn(List.of(citizen));
        when(userMapper.toDTOList(List.of(citizen))).thenReturn(List.of(new com.uit.scirs.auth.dto.UserDTO()));
        Report pendingReport = new Report();
        when(reportRepository.findTop10ByStatusOrderByCreatedAtDesc(ReportStatus.PENDING_APPROVAL))
                .thenReturn(List.of(pendingReport));
        when(reportMapper.toDTOList(List.of(pendingReport))).thenReturn(List.of(new ReportDTO()));

        AdminDashboardDTO dto = dashboardService.getAdminDashboard();

        assertThat(dto.getPendingAccountCount()).isEqualTo(4L);
        assertThat(dto.getPendingReportCount()).isEqualTo(6L);
        assertThat(dto.getRecentRegistrations()).hasSize(1);
        assertThat(dto.getReportsAwaitingApproval()).hasSize(1);
    }

    @Test
    void getStaffDashboard_whenCallerIsStaff_scopesToOwnDepartmentRegardlessOfRequestedId() {
        CurrentUser staff = new CurrentUser(50L, "staff@example.com", RoleName.STAFF, 2L);
        stubCountsAndSeries(2L);
        when(reportRepository.search(null, null, 2L)).thenReturn(List.of());

        dashboardService.getStaffDashboard(999L, staff);

        verify(reportRepository, org.mockito.Mockito.atLeastOnce()).countByDepartmentAndStatusIn(eq(2L), anyList());
        verify(reportRepository, never()).countByDepartmentAndStatusIn(eq(999L), anyList());
    }

    @Test
    void getStaffDashboard_whenCallerIsAdminWithNoDepartmentId_aggregatesGlobally() {
        CurrentUser admin = new CurrentUser(1L, "admin@example.com", RoleName.ADMIN, null);
        stubCountsAndSeries(null);
        when(reportRepository.search(null, null, null)).thenReturn(List.of());

        dashboardService.getStaffDashboard(null, admin);

        verify(reportRepository, org.mockito.Mockito.atLeastOnce()).countByDepartmentAndStatusIn(isNull(), anyList());
    }

    @Test
    void getStaffDashboard_buildsATwelveMonthSeriesZeroFillingMonthsWithNoReports() {
        CurrentUser admin = new CurrentUser(1L, "admin@example.com", RoleName.ADMIN, null);
        when(reportRepository.countByDepartmentAndStatusIn(isNull(), anyList())).thenReturn(0L);
        when(reportRepository.search(null, null, null)).thenReturn(List.of());
        when(reportMapper.toDTOList(anyList())).thenReturn(List.of());

        String currentMonth = YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        when(reportRepository.findCreatedAtSince(isNull(), any(LocalDateTime.class)))
                .thenReturn(List.of(LocalDateTime.now(), LocalDateTime.now()));

        StaffDashboardDTO dto = dashboardService.getStaffDashboard(null, admin);

        assertThat(dto.getMonthlySeries()).hasSize(12);
        assertThat(dto.getMonthlySeries().get(11).getMonth()).isEqualTo(currentMonth);
        assertThat(dto.getMonthlySeries().get(11).getCount()).isEqualTo(2L);
        assertThat(dto.getMonthlySeries().get(0).getCount()).isEqualTo(0L);
    }

    @Test
    void getDepartmentPerformance_defaultsMissingMetricsForADepartmentWithNoData() {
        Department roads = new Department();
        roads.setId(2L);
        roads.setName("Roads");
        when(departmentRepository.findAll()).thenReturn(List.of(roads));
        when(reportRepository.countOpenAndResolvedByDepartment(anyList(), anyList())).thenReturn(List.of());
        when(reportRepository.findResolutionTimes()).thenReturn(List.of());
        when(feedbackRepository.averageRatingByDepartment()).thenReturn(List.of());

        List<DepartmentPerformanceDTO> result = dashboardService.getDepartmentPerformance();

        assertThat(result).hasSize(1);
        DepartmentPerformanceDTO dto = result.get(0);
        assertThat(dto.getDepartmentId()).isEqualTo(2L);
        assertThat(dto.getOpenCount()).isZero();
        assertThat(dto.getResolvedCount()).isZero();
        assertThat(dto.getAverageResolutionHours()).isNull();
        assertThat(dto.getAverageRating()).isNull();
    }

    @Test
    void getDepartmentPerformance_computesResolutionHoursAndRatingWhenDataExists() {
        Department roads = new Department();
        roads.setId(2L);
        roads.setName("Roads");
        when(departmentRepository.findAll()).thenReturn(List.of(roads));

        DepartmentStatusCountProjection counts = statusCount(2L, 3L, 5L);
        when(reportRepository.countOpenAndResolvedByDepartment(anyList(), anyList())).thenReturn(List.of(counts));

        LocalDateTime approvedAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        LocalDateTime resolvedAt = approvedAt.plusHours(10);
        when(reportRepository.findResolutionTimes()).thenReturn(List.of(resolutionTime(2L, approvedAt, resolvedAt)));

        when(feedbackRepository.averageRatingByDepartment()).thenReturn(List.of(rating(2L, 4.5)));

        List<DepartmentPerformanceDTO> result = dashboardService.getDepartmentPerformance();

        DepartmentPerformanceDTO dto = result.get(0);
        assertThat(dto.getOpenCount()).isEqualTo(3L);
        assertThat(dto.getResolvedCount()).isEqualTo(5L);
        assertThat(dto.getAverageResolutionHours()).isEqualTo(10.0);
        assertThat(dto.getAverageRating()).isEqualTo(4.5);
    }

    @Test
    void getCategoryVolume_defaultsToZeroForACategoryWithNoReports() {
        Category pothole = new Category();
        pothole.setId(1L);
        pothole.setName("Pothole / Damaged Road");
        pothole.setColorHex("#F97316");
        when(categoryRepository.findAll()).thenReturn(List.of(pothole));
        when(reportRepository.countGroupedByCategory()).thenReturn(List.of());

        List<CategoryVolumeDTO> result = dashboardService.getCategoryVolume();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReportCount()).isZero();
        assertThat(result.get(0).getColorHex()).isEqualTo("#F97316");
    }

    private void stubCountsAndSeries(Long departmentId) {
        lenient().when(reportRepository.countByDepartmentAndStatusIn(eq(departmentId), anyList())).thenReturn(0L);
        lenient().when(reportRepository.findCreatedAtSince(eq(departmentId), any(LocalDateTime.class))).thenReturn(List.of());
        lenient().when(reportMapper.toDTOList(anyList())).thenReturn(List.of());
    }

    private DepartmentStatusCountProjection statusCount(Long departmentId, Long open, Long resolved) {
        return new DepartmentStatusCountProjection() {
            public Long getDepartmentId() { return departmentId; }
            public Long getOpenCount() { return open; }
            public Long getResolvedCount() { return resolved; }
        };
    }

    private ResolutionTimeProjection resolutionTime(Long departmentId, LocalDateTime approvedAt, LocalDateTime resolvedAt) {
        return new ResolutionTimeProjection() {
            public Long getDepartmentId() { return departmentId; }
            public LocalDateTime getApprovedAt() { return approvedAt; }
            public LocalDateTime getResolvedAt() { return resolvedAt; }
        };
    }

    private DepartmentRatingProjection rating(Long departmentId, Double averageRating) {
        return new DepartmentRatingProjection() {
            public Long getDepartmentId() { return departmentId; }
            public Double getAverageRating() { return averageRating; }
        };
    }
}
