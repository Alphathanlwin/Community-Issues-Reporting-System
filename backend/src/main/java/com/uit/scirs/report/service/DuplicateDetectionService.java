package com.uit.scirs.report.service;

import com.uit.scirs.report.dto.PossibleDuplicateDTO;
import com.uit.scirs.report.entity.ImageType;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Proximity-based duplicate detection: same category, non-terminal status,
 * within 100m of a candidate location. Pure geometric/SQL matching — not an
 * AI feature, so it doesn't conflict with the Out of Scope AI-duplicate-
 * detection exclusion in project-overview.md (see Decision D20).
 *
 * <p>No PostGIS on this project, so matching is a bounding-box pre-filter in
 * SQL (see {@link ReportRepository#findOpenCandidatesNear}) followed by a
 * precise Haversine distance calculation here in Java.
 */
@Service
public class DuplicateDetectionService {

    private static final double MATCH_RADIUS_METERS = 100.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final double METERS_PER_DEGREE_LATITUDE = 111_320.0;
    private static final int MAX_RESULTS = 5;

    private static final List<ReportStatus> TERMINAL_STATUSES =
            List.of(ReportStatus.RESOLVED, ReportStatus.CLOSED, ReportStatus.REJECTED);

    private final ReportRepository reportRepository;

    public DuplicateDetectionService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public List<PossibleDuplicateDTO> findPossibleDuplicates(BigDecimal latitude, BigDecimal longitude, Long categoryId) {
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();

        double latDeltaDeg = MATCH_RADIUS_METERS / METERS_PER_DEGREE_LATITUDE;
        double metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * Math.cos(Math.toRadians(lat));
        double lngDeltaDeg = MATCH_RADIUS_METERS / Math.max(metersPerDegreeLongitude, 1.0);

        List<Report> candidates = reportRepository.findOpenCandidatesNear(
                categoryId,
                TERMINAL_STATUSES,
                BigDecimal.valueOf(lat - latDeltaDeg),
                BigDecimal.valueOf(lat + latDeltaDeg),
                BigDecimal.valueOf(lng - lngDeltaDeg),
                BigDecimal.valueOf(lng + lngDeltaDeg));

        return candidates.stream()
                .map(report -> toDTO(report, distanceMeters(lat, lng,
                        report.getLatitude().doubleValue(), report.getLongitude().doubleValue())))
                .filter(dto -> dto.getDistanceMeters() <= MATCH_RADIUS_METERS)
                .sorted(Comparator.comparingDouble(PossibleDuplicateDTO::getDistanceMeters))
                .limit(MAX_RESULTS)
                .toList();
    }

    private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private PossibleDuplicateDTO toDTO(Report report, double distanceMeters) {
        PossibleDuplicateDTO dto = new PossibleDuplicateDTO();
        dto.setReportId(report.getId());
        dto.setReportCode(report.getReportCode());
        dto.setTitle(report.getTitle());
        dto.setStatus(report.getStatus().name());
        dto.setDistanceMeters(Math.round(distanceMeters * 10.0) / 10.0);
        dto.setCreatedAt(report.getCreatedAt());
        report.getImages().stream()
                .filter(image -> image.getImageType() == ImageType.REPORT_PHOTO)
                .findFirst()
                .ifPresent(image -> dto.setThumbnailUrl(image.getImageUrl()));
        return dto;
    }
}
