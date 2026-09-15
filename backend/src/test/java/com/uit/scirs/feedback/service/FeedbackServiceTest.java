package com.uit.scirs.feedback.service;

import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.feedback.dto.CreateFeedbackDTO;
import com.uit.scirs.feedback.dto.FeedbackDTO;
import com.uit.scirs.feedback.entity.Feedback;
import com.uit.scirs.feedback.mapper.FeedbackMapper;
import com.uit.scirs.feedback.repository.FeedbackRepository;
import com.uit.scirs.notification.service.NotificationService;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.report.service.StatusHistoryService;
import com.uit.scirs.score.entity.PointReason;
import com.uit.scirs.score.service.ScoreService;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock FeedbackRepository feedbackRepository;
    @Mock ReportRepository reportRepository;
    @Mock UserRepository userRepository;
    @Mock FeedbackMapper feedbackMapper;
    @Mock ScoreService scoreService;
    @Mock StatusHistoryService statusHistoryService;
    @Mock NotificationService notificationService;
    @InjectMocks FeedbackService feedbackService;

    @Test
    void create_onNonResolvedReport_throwsBusinessRuleException() {
        Report report = report(1L, ReportStatus.IN_PROGRESS, 7L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> feedbackService.create(feedbackDto(1L), 7L))
                .isInstanceOf(BusinessRuleException.class);

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void create_whenFeedbackAlreadyExists_throwsDuplicateResourceException() {
        Report report = report(1L, ReportStatus.RESOLVED, 7L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(feedbackRepository.existsByReportId(1L)).thenReturn(true);

        assertThatThrownBy(() -> feedbackService.create(feedbackDto(1L), 7L))
                .isInstanceOf(DuplicateResourceException.class);

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void create_fromNonReporter_throwsAccessDeniedException() {
        Report report = report(1L, ReportStatus.RESOLVED, 7L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> feedbackService.create(feedbackDto(1L), 99L))
                .isInstanceOf(AccessDeniedException.class);

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void create_onResolvedReportByReporter_savesAndAwardsFivePointsOnce() {
        Report report = report(1L, ReportStatus.RESOLVED, 7L);
        User citizen = report.getReporter();

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(feedbackRepository.existsByReportId(1L)).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));
        when(feedbackMapper.toDTO(any(Feedback.class))).thenReturn(new FeedbackDTO());
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));

        feedbackService.create(feedbackDto(1L), 7L);

        verify(scoreService).award(citizen, PointReason.FEEDBACK_GIVEN, report);
        // A resolved report is done once the citizen has weighed in.
        assertThat(report.getStatus()).isEqualTo(ReportStatus.CLOSED);
        verify(statusHistoryService).record(report, ReportStatus.RESOLVED, ReportStatus.CLOSED, citizen,
                "Closed after citizen feedback.");
        verify(notificationService).notifyStatusChange(report, 5);
    }

    @Test
    void create_onAlreadyClosedReport_leavesStatusAloneButNotifiesStaffOfFeedback() {
        Report report = report(1L, ReportStatus.CLOSED, 7L);
        User citizen = report.getReporter();

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(feedbackRepository.existsByReportId(1L)).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));
        when(feedbackMapper.toDTO(any(Feedback.class))).thenReturn(new FeedbackDTO());

        feedbackService.create(feedbackDto(1L), 7L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.CLOSED);
        verify(reportRepository, never()).save(any(Report.class));
        verify(notificationService, never()).notifyStatusChange(any());
        verify(notificationService, never()).notifyStatusChange(any(), any());
        // Staff closed it without waiting for feedback — the department still needs
        // to hear about the rating that just came in.
        verify(notificationService).notifyFeedbackReceived(report, 5, "Fixed quickly, thank you!");
    }

    private Report report(Long id, ReportStatus status, Long reporterId) {
        Report report = new Report();
        report.setId(id);
        report.setStatus(status);
        User reporter = new User();
        reporter.setId(reporterId);
        reporter.setFullName("Test Citizen");
        report.setReporter(reporter);
        return report;
    }

    private CreateFeedbackDTO feedbackDto(Long reportId) {
        CreateFeedbackDTO dto = new CreateFeedbackDTO();
        dto.setReportId(reportId);
        dto.setRating(5);
        dto.setComment("Fixed quickly, thank you!");
        return dto;
    }
}
