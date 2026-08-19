package com.uit.scirs.dashboard.service;

import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.dashboard.dto.AdminDashboardDTO;
import com.uit.scirs.dashboard.dto.CategoryVolumeDTO;
import com.uit.scirs.dashboard.dto.DepartmentPerformanceDTO;
import com.uit.scirs.dashboard.dto.MonthlyReportCountDTO;
import com.uit.scirs.dashboard.dto.StaffDashboardDTO;
import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.repository.DepartmentRepository;
import com.uit.scirs.feedback.repository.DepartmentRatingProjection;
import com.uit.scirs.feedback.repository.FeedbackRepository;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.mapper.ReportMapper;
import com.uit.scirs.report.repository.CategoryVolumeProjection;
import com.uit.scirs.report.repository.DepartmentStatusCountProjection;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.report.repository.ResolutionTimeProjection;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.mapper.UserMapper;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final List<ReportStatus> OPEN_STATUSES = List.of(ReportStatus.ASSIGNED, ReportStatus.IN_PROGRESS);
    private static final List<ReportStatus> RESOLVED_STATUSES = List.of(ReportStatus.RESOLVED, ReportStatus.CLOSED);
    private static final List<ReportStatus> NEW_STATUSES = List.of(ReportStatus.ASSIGNED);
    private static final List<ReportStatus> DEPARTMENT_SCOPED_STATUSES =
            List.of(ReportStatus.ASSIGNED, ReportStatus.IN_PROGRESS, ReportStatus.RESOLVED, ReportStatus.CLOSED);
    private static final int MONTHLY_SERIES_LENGTH = 12;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final ReportMapper reportMapper;
    private final UserMapper userMapper;

    public DashboardService(ReportRepository reportRepository,
                             UserRepository userRepository,
                             FeedbackRepository feedbackRepository,
                             DepartmentRepository departmentRepository,
                             CategoryRepository categoryRepository,
                             ReportMapper reportMapper,
                             UserMapper userMapper) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.feedbackRepository = feedbackRepository;
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.reportMapper = reportMapper;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public AdminDashboardDTO getAdminDashboard() {
        AdminDashboardDTO dto = new AdminDashboardDTO();
        dto.setPendingAccountCount(userRepository.countByAccountStatus(AccountStatus.PENDING));
        dto.setPendingReportCount(reportRepository.countByStatus(ReportStatus.PENDING_APPROVAL));
        dto.setRecentRegistrations(
                userMapper.toDTOList(userRepository.findTop10ByRoleNameOrderByCreatedAtDesc(RoleName.CITIZEN)));
        dto.setReportsAwaitingApproval(
                reportMapper.toDTOList(reportRepository.findTop10ByStatusOrderByCreatedAtDesc(ReportStatus.PENDING_APPROVAL)));
        return dto;
    }

    /**
     * Staff are always scoped to their own department (ownership rule); an
     * admin may optionally pass departmentId, or omit it to aggregate across
     * every department.
     */
    @Transactional(readOnly = true)
    public StaffDashboardDTO getStaffDashboard(Long requestedDepartmentId, CurrentUser currentUser) {
        Long departmentId = currentUser.getRole() == RoleName.STAFF
                ? currentUser.getDepartmentId()
                : requestedDepartmentId;

        StaffDashboardDTO dto = new StaffDashboardDTO();
        dto.setTotalReports(reportRepository.countByDepartmentAndStatusIn(departmentId, DEPARTMENT_SCOPED_STATUSES));
        dto.setResolvedReports(reportRepository.countByDepartmentAndStatusIn(departmentId, RESOLVED_STATUSES));
        dto.setRemainingReports(reportRepository.countByDepartmentAndStatusIn(departmentId, OPEN_STATUSES));
        dto.setNewReports(reportRepository.countByDepartmentAndStatusIn(departmentId, NEW_STATUSES));
        dto.setMonthlySeries(buildMonthlySeries(departmentId));

        List<Report> recent = reportRepository.search(null, null, departmentId);
        dto.setRecentReports(reportMapper.toDTOList(recent.stream().limit(10).toList()));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<DepartmentPerformanceDTO> getDepartmentPerformance() {
        Map<Long, DepartmentStatusCountProjection> statusCounts = reportRepository
                .countOpenAndResolvedByDepartment(OPEN_STATUSES, RESOLVED_STATUSES).stream()
                .collect(Collectors.toMap(DepartmentStatusCountProjection::getDepartmentId, p -> p));

        Map<Long, Double> avgResolutionHours = reportRepository.findResolutionTimes().stream()
                .collect(Collectors.groupingBy(ResolutionTimeProjection::getDepartmentId,
                        Collectors.averagingDouble(this::resolutionHours)));

        Map<Long, Double> avgRatings = feedbackRepository.averageRatingByDepartment().stream()
                .collect(Collectors.toMap(DepartmentRatingProjection::getDepartmentId,
                        DepartmentRatingProjection::getAverageRating));

        return departmentRepository.findAll().stream()
                .map(department -> toPerformanceDTO(department, statusCounts, avgResolutionHours, avgRatings))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryVolumeDTO> getCategoryVolume() {
        Map<Long, Long> counts = reportRepository.countGroupedByCategory().stream()
                .collect(Collectors.toMap(CategoryVolumeProjection::getCategoryId, CategoryVolumeProjection::getTotal));

        return categoryRepository.findAll().stream()
                .map(category -> toVolumeDTO(category, counts))
                .toList();
    }

    private double resolutionHours(ResolutionTimeProjection projection) {
        return Duration.between(projection.getApprovedAt(), projection.getResolvedAt()).toMinutes() / 60.0;
    }

    private List<MonthlyReportCountDTO> buildMonthlySeries(Long departmentId) {
        YearMonth start = YearMonth.now().minusMonths(MONTHLY_SERIES_LENGTH - 1);
        LocalDateTime since = start.atDay(1).atStartOfDay();

        Map<String, Long> counts = new HashMap<>();
        for (LocalDateTime createdAt : reportRepository.findCreatedAtSince(departmentId, since)) {
            String month = YearMonth.from(createdAt).format(MONTH_FORMATTER);
            counts.merge(month, 1L, Long::sum);
        }

        List<MonthlyReportCountDTO> series = new ArrayList<>(MONTHLY_SERIES_LENGTH);
        YearMonth cursor = start;
        for (int i = 0; i < MONTHLY_SERIES_LENGTH; i++) {
            String label = cursor.format(MONTH_FORMATTER);
            MonthlyReportCountDTO point = new MonthlyReportCountDTO();
            point.setMonth(label);
            point.setCount(counts.getOrDefault(label, 0L));
            series.add(point);
            cursor = cursor.plusMonths(1);
        }
        return series;
    }

    private DepartmentPerformanceDTO toPerformanceDTO(Department department,
                                                        Map<Long, DepartmentStatusCountProjection> statusCounts,
                                                        Map<Long, Double> avgResolutionHours,
                                                        Map<Long, Double> avgRatings) {
        DepartmentPerformanceDTO dto = new DepartmentPerformanceDTO();
        dto.setDepartmentId(department.getId());
        dto.setDepartmentName(department.getName());

        DepartmentStatusCountProjection counts = statusCounts.get(department.getId());
        dto.setOpenCount(counts != null ? counts.getOpenCount() : 0L);
        dto.setResolvedCount(counts != null ? counts.getResolvedCount() : 0L);
        dto.setAverageResolutionHours(avgResolutionHours.get(department.getId()));
        dto.setAverageRating(avgRatings.get(department.getId()));
        return dto;
    }

    private CategoryVolumeDTO toVolumeDTO(Category category, Map<Long, Long> counts) {
        CategoryVolumeDTO dto = new CategoryVolumeDTO();
        dto.setCategoryId(category.getId());
        dto.setCategoryName(category.getName());
        dto.setColorHex(category.getColorHex());
        dto.setReportCount(counts.getOrDefault(category.getId(), 0L));
        return dto;
    }
}
