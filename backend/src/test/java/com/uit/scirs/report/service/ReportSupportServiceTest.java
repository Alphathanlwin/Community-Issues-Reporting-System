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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportSupportServiceTest {

    @Mock ReportSupportRepository reportSupportRepository;
    @Mock ReportRepository reportRepository;
    @Mock UserRepository userRepository;
    @Mock ScoreService scoreService;
    @InjectMocks ReportSupportService reportSupportService;

    @Test
    void support_freshReport_savesSupportAndAwardsFivePoints() {
        Report report = report(1L);
        User citizen = user(7L);

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportSupportRepository.existsByReportIdAndCitizenId(1L, 7L)).thenReturn(false);
        when(reportSupportRepository.countByCitizenIdAndCreatedAtAfter(eq(7L), any())).thenReturn(0L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(scoreService.awardRepeatable(citizen, PointReason.SUPPORT_GIVEN, report)).thenReturn(55);
        when(reportSupportRepository.countByReportId(1L)).thenReturn(3L);

        ReportSupportResultDTO result = reportSupportService.support(1L, 7L);

        assertThat(result.getReportId()).isEqualTo(1L);
        assertThat(result.getSupportCount()).isEqualTo(3L);
        assertThat(result.getAwardedPoints()).isEqualTo(5);
        assertThat(result.getTotalPoints()).isEqualTo(55);
        assertThat(result.getRemainingToday()).isEqualTo(4L);
        verify(reportSupportRepository).save(any(ReportSupport.class));
    }

    @Test
    void support_alreadySupported_throwsDuplicateResourceException() {
        Report report = report(1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportSupportRepository.existsByReportIdAndCitizenId(1L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> reportSupportService.support(1L, 7L))
                .isInstanceOf(DuplicateResourceException.class);

        verify(reportSupportRepository, never()).save(any(ReportSupport.class));
    }

    @Test
    void support_dailyLimitReached_throwsBusinessRuleException() {
        Report report = report(1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportSupportRepository.existsByReportIdAndCitizenId(1L, 7L)).thenReturn(false);
        when(reportSupportRepository.countByCitizenIdAndCreatedAtAfter(eq(7L), any())).thenReturn(5L);

        assertThatThrownBy(() -> reportSupportService.support(1L, 7L))
                .isInstanceOf(BusinessRuleException.class);

        verify(reportSupportRepository, never()).save(any(ReportSupport.class));
    }

    @Test
    void support_unknownReport_throwsResourceNotFoundException() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportSupportService.support(99L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeSupport_existingSupport_deletesAndWithdrawsFivePoints() {
        Report report = report(1L);
        ReportSupport existing = new ReportSupport();
        existing.setReport(report);
        User citizen = user(7L);

        when(reportSupportRepository.findByReportIdAndCitizenId(1L, 7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(scoreService.awardRepeatable(citizen, PointReason.SUPPORT_WITHDRAWN, report)).thenReturn(50);
        when(reportSupportRepository.countByReportId(1L)).thenReturn(2L);
        when(reportSupportRepository.countByCitizenIdAndCreatedAtAfter(eq(7L), any())).thenReturn(1L);

        ReportSupportResultDTO result = reportSupportService.removeSupport(1L, 7L);

        assertThat(result.getAwardedPoints()).isEqualTo(-5);
        assertThat(result.getTotalPoints()).isEqualTo(50);
        assertThat(result.getSupportCount()).isEqualTo(2L);
        assertThat(result.getRemainingToday()).isEqualTo(4L);
        verify(reportSupportRepository).delete(existing);
    }

    @Test
    void removeSupport_noExistingSupport_throwsBusinessRuleException() {
        when(reportSupportRepository.findByReportIdAndCitizenId(1L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportSupportService.removeSupport(1L, 7L))
                .isInstanceOf(BusinessRuleException.class);

        verify(reportSupportRepository, never()).delete(any(ReportSupport.class));
    }

    private Report report(Long id) {
        Report report = new Report();
        report.setId(id);
        return report;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setScorePoints(0);
        return user;
    }
}
