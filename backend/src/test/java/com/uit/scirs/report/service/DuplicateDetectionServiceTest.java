package com.uit.scirs.report.service;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.report.dto.PossibleDuplicateDTO;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceTest {

    @Mock ReportRepository reportRepository;
    @InjectMocks DuplicateDetectionService duplicateDetectionService;

    @Test
    void findPossibleDuplicates_candidateWithinRadius_isReturnedWithDistance() {
        // ~18m north of the search point.
        Report nearby = report(1L, "16.8409000", "96.1735000", ReportStatus.ASSIGNED);
        when(reportRepository.findOpenCandidatesNear(eq(3L), anyList(), any(), any(), any(), any()))
                .thenReturn(List.of(nearby));

        List<PossibleDuplicateDTO> result = duplicateDetectionService.findPossibleDuplicates(
                new BigDecimal("16.8407000"), new BigDecimal("96.1735000"), 3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReportId()).isEqualTo(1L);
        assertThat(result.get(0).getDistanceMeters()).isLessThan(100.0);
    }

    @Test
    void findPossibleDuplicates_candidateOutsidePreciseRadius_isExcludedDespiteBoundingBox() {
        // The bounding box is a square approximation of the 100m circle, so a
        // candidate can sit inside the box (within both the ~0.000898 deg lat
        // and ~0.000939 deg lng half-widths) but ~130.7m away by Haversine —
        // the precise Java pass must still exclude it.
        Report farCorner = report(2L, "16.8417000", "96.1744000", ReportStatus.ASSIGNED);
        when(reportRepository.findOpenCandidatesNear(eq(3L), anyList(), any(), any(), any(), any()))
                .thenReturn(List.of(farCorner));

        List<PossibleDuplicateDTO> result = duplicateDetectionService.findPossibleDuplicates(
                new BigDecimal("16.8409000"), new BigDecimal("96.1735000"), 3L);

        assertThat(result).isEmpty();
    }

    @Test
    void findPossibleDuplicates_excludesTerminalStatuses_viaRepositoryQueryArgument() {
        when(reportRepository.findOpenCandidatesNear(eq(3L), anyList(), any(), any(), any(), any()))
                .thenReturn(List.of());

        duplicateDetectionService.findPossibleDuplicates(new BigDecimal("16.8409000"), new BigDecimal("96.1735000"), 3L);

        ArgumentCaptor<List<ReportStatus>> excluded = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(reportRepository)
                .findOpenCandidatesNear(eq(3L), excluded.capture(), any(), any(), any(), any());
        assertThat(excluded.getValue()).containsExactlyInAnyOrder(
                ReportStatus.RESOLVED, ReportStatus.CLOSED, ReportStatus.REJECTED);
    }

    @Test
    void findPossibleDuplicates_sortsByDistanceAndCapsAtFive() {
        List<Report> candidates = List.of(
                report(1L, "16.8412000", "96.1735000", ReportStatus.ASSIGNED), // ~33m
                report(2L, "16.8409000", "96.1735900", ReportStatus.ASSIGNED), // ~9.5m
                report(3L, "16.8410000", "96.1735500", ReportStatus.ASSIGNED), // closer than #1
                report(4L, "16.8411000", "96.1735000", ReportStatus.ASSIGNED),
                report(5L, "16.8409500", "96.1735000", ReportStatus.ASSIGNED),
                report(6L, "16.8409200", "96.1735000", ReportStatus.ASSIGNED));
        when(reportRepository.findOpenCandidatesNear(eq(3L), anyList(), any(), any(), any(), any()))
                .thenReturn(candidates);

        List<PossibleDuplicateDTO> result = duplicateDetectionService.findPossibleDuplicates(
                new BigDecimal("16.8409000"), new BigDecimal("96.1735000"), 3L);

        assertThat(result).hasSize(5);
        assertThat(result).isSortedAccordingTo(
                java.util.Comparator.comparingDouble(PossibleDuplicateDTO::getDistanceMeters));
    }

    private Report report(Long id, String lat, String lng, ReportStatus status) {
        Report report = new Report();
        report.setId(id);
        report.setReportCode("RPT-DUP-" + id);
        report.setTitle("Pothole");
        report.setStatus(status);
        report.setCategory(new Category());
        report.setLatitude(new BigDecimal(lat));
        report.setLongitude(new BigDecimal(lng));
        return report;
    }
}
