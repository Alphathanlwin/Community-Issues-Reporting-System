package com.uit.scirs.report.repository;

import com.uit.scirs.report.entity.ReportConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportConfirmationRepository extends JpaRepository<ReportConfirmation, Long> {

    boolean existsByReportIdAndCitizenId(Long reportId, Long citizenId);
}
