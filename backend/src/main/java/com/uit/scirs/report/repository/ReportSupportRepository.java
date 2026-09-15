package com.uit.scirs.report.repository;

import com.uit.scirs.report.entity.ReportSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReportSupportRepository extends JpaRepository<ReportSupport, Long> {

    boolean existsByReportIdAndCitizenId(Long reportId, Long citizenId);

    Optional<ReportSupport> findByReportIdAndCitizenId(Long reportId, Long citizenId);

    long countByReportId(Long reportId);

    // Rolling 24h window (not calendar-day), matching the citizen home feed's
    // existing "DAILY_SUPPORT_LIMIT" convention (supportEngine.js).
    long countByCitizenIdAndCreatedAtAfter(Long citizenId, LocalDateTime since);
}
