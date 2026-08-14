package com.uit.scirs.report.repository;

import com.uit.scirs.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReportCode(String reportCode);

    boolean existsByReportCode(String reportCode);
}
