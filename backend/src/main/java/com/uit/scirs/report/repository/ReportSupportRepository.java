package com.uit.scirs.report.repository;

import com.uit.scirs.report.entity.ReportSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReportSupportRepository extends JpaRepository<ReportSupport, Long> {

    boolean existsByReportIdAndCitizenId(Long reportId, Long citizenId);

    // Removes this citizen's support for the report (the "un-support" toggle);
    // returns how many rows were deleted (0 or 1).
    long deleteByReportIdAndCitizenId(Long reportId, Long citizenId);

    long countByReportId(Long reportId);

    // Backs the rolling-24h daily cap: supports this citizen has recorded since `since`.
    long countByCitizenIdAndCreatedAtAfter(Long citizenId, LocalDateTime since);
}
