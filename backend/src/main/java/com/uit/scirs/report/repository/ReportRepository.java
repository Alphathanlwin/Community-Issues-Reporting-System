package com.uit.scirs.report.repository;

import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReportCode(String reportCode);

    boolean existsByReportCode(String reportCode);

    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<Report> findByStatusInAndUpdatedAtBefore(List<ReportStatus> statuses, LocalDateTime cutoff);

    // Optional filters: any null parameter is skipped. Staff scoping is
    // enforced by the service always supplying its own departmentId.
    @Query("""
           SELECT r FROM Report r
           WHERE (:status IS NULL OR r.status = :status)
             AND (:categoryId IS NULL OR r.category.id = :categoryId)
             AND (:departmentId IS NULL OR r.department.id = :departmentId)
           ORDER BY r.createdAt DESC
           """)
    List<Report> search(ReportStatus status, Long categoryId, Long departmentId);
}
