package com.uit.scirs.report.service;

import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.report.dto.ReportSupportResultDTO;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportSupport;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.report.repository.ReportSupportRepository;
import com.uit.scirs.score.entity.PointReason;
import com.uit.scirs.score.service.ScoreService;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * The community-feed "Support" heart button (`POST`/`DELETE /api/reports/{id}/support`,
 * matching the frontend's communitySupportApi.js contract) — a reversible,
 * repeatable endorsement, rate-limited per citizen per rolling 24h window.
 * Distinct from {@link ReportAssignmentService}'s reassignment and from a
 * {@code ReportConfirmation} (given once, only during report submission).
 */
@Service
public class ReportSupportService {

    // Matches the frontend's DAILY_SUPPORT_LIMIT (supportEngine.js) and the
    // SUPPORT_GIVEN/SUPPORT_WITHDRAWN point values in ScoreService.
    private static final int DAILY_SUPPORT_LIMIT = 5;

    private final ReportSupportRepository reportSupportRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ScoreService scoreService;

    public ReportSupportService(ReportSupportRepository reportSupportRepository,
                                 ReportRepository reportRepository,
                                 UserRepository userRepository,
                                 ScoreService scoreService) {
        this.reportSupportRepository = reportSupportRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.scoreService = scoreService;
    }

    @Transactional
    public ReportSupportResultDTO support(Long reportId, Long citizenId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        if (reportSupportRepository.existsByReportIdAndCitizenId(reportId, citizenId)) {
            throw new DuplicateResourceException("You already supported this report.");
        }

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long usedToday = reportSupportRepository.countByCitizenIdAndCreatedAtAfter(citizenId, since);
        if (usedToday >= DAILY_SUPPORT_LIMIT) {
            throw new BusinessRuleException("You've reached today's support limit. Try again tomorrow.");
        }

        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ReportSupport support = new ReportSupport();
        support.setReport(report);
        support.setCitizen(citizen);
        reportSupportRepository.save(support);

        int totalPoints = scoreService.awardRepeatable(citizen, PointReason.SUPPORT_GIVEN, report);
        long supportCount = reportSupportRepository.countByReportId(reportId);
        long remainingToday = DAILY_SUPPORT_LIMIT - (usedToday + 1);

        return result(reportId, supportCount, 5, totalPoints, remainingToday);
    }

    @Transactional
    public ReportSupportResultDTO removeSupport(Long reportId, Long citizenId) {
        ReportSupport existing = reportSupportRepository.findByReportIdAndCitizenId(reportId, citizenId)
                .orElseThrow(() -> new BusinessRuleException("You haven't supported this report."));

        Report report = existing.getReport();
        reportSupportRepository.delete(existing);

        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        int totalPoints = scoreService.awardRepeatable(citizen, PointReason.SUPPORT_WITHDRAWN, report);
        long supportCount = reportSupportRepository.countByReportId(reportId);

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long usedToday = reportSupportRepository.countByCitizenIdAndCreatedAtAfter(citizenId, since);
        long remainingToday = Math.max(0, DAILY_SUPPORT_LIMIT - usedToday);

        return result(reportId, supportCount, -5, totalPoints, remainingToday);
    }

    private ReportSupportResultDTO result(Long reportId, long supportCount, int awardedPoints,
                                           int totalPoints, long remainingToday) {
        ReportSupportResultDTO dto = new ReportSupportResultDTO();
        dto.setReportId(reportId);
        dto.setSupportCount(supportCount);
        dto.setAwardedPoints(awardedPoints);
        dto.setTotalPoints(totalPoints);
        dto.setRemainingToday(remainingToday);
        return dto;
    }
}
