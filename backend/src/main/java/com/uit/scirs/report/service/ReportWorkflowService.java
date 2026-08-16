package com.uit.scirs.report.service;

import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.InvalidStatusTransitionException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.notification.service.NotificationService;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.dto.UpdateReportStatusDTO;
import com.uit.scirs.report.entity.ImageType;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.mapper.ReportMapper;
import com.uit.scirs.report.repository.ReportImageRepository;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.score.entity.PointReason;
import com.uit.scirs.score.service.ScoreService;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * The only place {@code report.status} is mutated (see code-standards.md's
 * anti-pattern rule) — every transition here writes history, awards points
 * where the scoring rules apply, and notifies the reporter.
 */
@Service
public class ReportWorkflowService {

    // Transitions reachable via /status. PENDING_APPROVAL is deliberately
    // excluded — that source status can only be left via /approve or /reject,
    // which carry their own auto-routing / reason requirements.
    private static final Map<ReportStatus, Set<ReportStatus>> ALLOWED_STATUS_CHANGES = Map.of(
            ReportStatus.ASSIGNED, Set.of(ReportStatus.IN_PROGRESS, ReportStatus.RESOLVED),
            ReportStatus.IN_PROGRESS, Set.of(ReportStatus.RESOLVED),
            ReportStatus.RESOLVED, Set.of(ReportStatus.CLOSED, ReportStatus.IN_PROGRESS),
            ReportStatus.CLOSED, Set.of());

    private final ReportRepository reportRepository;
    private final ReportImageRepository reportImageRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;
    private final StatusHistoryService statusHistoryService;
    private final ScoreService scoreService;
    private final NotificationService notificationService;

    public ReportWorkflowService(ReportRepository reportRepository,
                                  ReportImageRepository reportImageRepository,
                                  UserRepository userRepository,
                                  ReportMapper reportMapper,
                                  StatusHistoryService statusHistoryService,
                                  ScoreService scoreService,
                                  NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.reportImageRepository = reportImageRepository;
        this.userRepository = userRepository;
        this.reportMapper = reportMapper;
        this.statusHistoryService = statusHistoryService;
        this.scoreService = scoreService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReportDTO approve(Long reportId, CurrentUser admin) {
        Report report = findEntity(reportId);
        if (report.getStatus() != ReportStatus.PENDING_APPROVAL) {
            throw new InvalidStatusTransitionException("Cannot approve a report in status " + report.getStatus());
        }

        User adminUser = findUser(admin.getId());
        ReportStatus oldStatus = report.getStatus();

        report.setDepartment(report.getCategory().getDepartment());
        report.setStatus(ReportStatus.ASSIGNED);
        report.setApprovedAt(LocalDateTime.now());
        report.setApprovedBy(adminUser);

        Report saved = reportRepository.save(report);
        statusHistoryService.record(saved, oldStatus, ReportStatus.ASSIGNED, adminUser, null);
        scoreService.award(saved.getReporter(), PointReason.REPORT_APPROVED, saved);
        notificationService.notifyStatusChange(saved);

        return reportMapper.toDTO(saved);
    }

    @Transactional
    public ReportDTO reject(Long reportId, String rejectionReason, CurrentUser admin) {
        Report report = findEntity(reportId);
        if (report.getStatus() != ReportStatus.PENDING_APPROVAL) {
            throw new InvalidStatusTransitionException("Cannot reject a report in status " + report.getStatus());
        }

        User adminUser = findUser(admin.getId());
        ReportStatus oldStatus = report.getStatus();

        report.setStatus(ReportStatus.REJECTED);
        report.setRejectionReason(rejectionReason);

        Report saved = reportRepository.save(report);
        statusHistoryService.record(saved, oldStatus, ReportStatus.REJECTED, adminUser, rejectionReason);
        scoreService.award(saved.getReporter(), PointReason.REPORT_REJECTED, saved);
        notificationService.notifyStatusChange(saved);

        return reportMapper.toDTO(saved);
    }

    @Transactional
    public ReportDTO changeStatus(Long reportId, UpdateReportStatusDTO dto, CurrentUser currentUser) {
        Report report = findEntity(reportId);
        assertStaffOwnsDepartment(report, currentUser);

        ReportStatus newStatus = parseStatus(dto.getStatus());
        ReportStatus oldStatus = report.getStatus();

        Set<ReportStatus> allowed = ALLOWED_STATUS_CHANGES.getOrDefault(oldStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidStatusTransitionException("Cannot change status from " + oldStatus + " to " + newStatus);
        }

        if (newStatus == ReportStatus.RESOLVED
                && reportImageRepository.findByReportIdAndImageType(reportId, ImageType.RESOLUTION_PHOTO).isEmpty()) {
            throw new BusinessRuleException(
                    "At least one completion photo is required before a report can be marked resolved.");
        }

        User changedBy = findUser(currentUser.getId());
        report.setStatus(newStatus);

        if (newStatus == ReportStatus.RESOLVED) {
            report.setResolvedAt(LocalDateTime.now());
        } else if (newStatus == ReportStatus.CLOSED) {
            report.setClosedAt(LocalDateTime.now());
        }

        Report saved = reportRepository.save(report);
        statusHistoryService.record(saved, oldStatus, newStatus, changedBy, dto.getRemarks());

        if (newStatus == ReportStatus.RESOLVED) {
            scoreService.award(saved.getReporter(), PointReason.REPORT_RESOLVED, saved);
        }

        notificationService.notifyStatusChange(saved);

        return reportMapper.toDTO(saved);
    }

    private void assertStaffOwnsDepartment(Report report, CurrentUser currentUser) {
        if (currentUser.getRole() == RoleName.STAFF) {
            Long staffDepartmentId = currentUser.getDepartmentId();
            Long reportDepartmentId = report.getDepartment() != null ? report.getDepartment().getId() : null;
            if (staffDepartmentId == null || !staffDepartmentId.equals(reportDepartmentId)) {
                throw new AccessDeniedException("This report belongs to another department.");
            }
        }
    }

    private ReportStatus parseStatus(String status) {
        try {
            return ReportStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Unknown report status: " + status);
        }
    }

    private Report findEntity(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
