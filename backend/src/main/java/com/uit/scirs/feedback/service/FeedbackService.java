package com.uit.scirs.feedback.service;

import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.feedback.dto.CreateFeedbackDTO;
import com.uit.scirs.feedback.dto.FeedbackDTO;
import com.uit.scirs.feedback.entity.Feedback;
import com.uit.scirs.feedback.mapper.FeedbackMapper;
import com.uit.scirs.feedback.repository.FeedbackRepository;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.score.entity.PointReason;
import com.uit.scirs.score.service.ScoreService;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
public class FeedbackService {

    private static final Set<ReportStatus> FEEDBACK_ELIGIBLE_STATUSES =
            EnumSet.of(ReportStatus.RESOLVED, ReportStatus.CLOSED);

    private final FeedbackRepository feedbackRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final FeedbackMapper feedbackMapper;
    private final ScoreService scoreService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                            ReportRepository reportRepository,
                            UserRepository userRepository,
                            FeedbackMapper feedbackMapper,
                            ScoreService scoreService) {
        this.feedbackRepository = feedbackRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.feedbackMapper = feedbackMapper;
        this.scoreService = scoreService;
    }

    @Transactional
    public FeedbackDTO create(CreateFeedbackDTO dto, Long citizenId) {
        Report report = reportRepository.findById(dto.getReportId())
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + dto.getReportId()));

        if (!report.getReporter().getId().equals(citizenId)) {
            throw new AccessDeniedException("You can only leave feedback on your own reports.");
        }

        if (!FEEDBACK_ELIGIBLE_STATUSES.contains(report.getStatus())) {
            throw new BusinessRuleException("Feedback can only be left on a resolved report.");
        }

        if (feedbackRepository.existsByReportId(report.getId())) {
            throw new DuplicateResourceException("Feedback has already been submitted for this report.");
        }

        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Feedback feedback = new Feedback();
        feedback.setReport(report);
        feedback.setCitizen(citizen);
        feedback.setRating(dto.getRating());
        feedback.setComment(dto.getComment());

        Feedback saved = feedbackRepository.save(feedback);
        scoreService.award(citizen, PointReason.FEEDBACK_GIVEN, report);

        return feedbackMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public FeedbackDTO getByReportId(Long reportId, CurrentUser currentUser) {
        Feedback feedback = feedbackRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("No feedback found for report id: " + reportId));

        boolean isOwner = feedback.getCitizen().getId().equals(currentUser.getId());
        boolean isStaffOrAdmin = currentUser.getRole() == RoleName.ADMIN || currentUser.getRole() == RoleName.STAFF;
        if (!isOwner && !isStaffOrAdmin) {
            throw new AccessDeniedException("You can only view feedback on your own reports.");
        }

        return feedbackMapper.toDTO(feedback);
    }
}
