package com.uit.scirs.report.repository;

import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReportCode(String reportCode);

    boolean existsByReportCode(String reportCode);

    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<Report> findTop10ByStatusOrderByCreatedAtDesc(ReportStatus status);

    long countByStatus(ReportStatus status);

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

    // Optional filters: category/status/bounding-box parameters are skipped when null.
    // restrictToPublic=true (citizens) additionally excludes hiddenStatuses.
    @Query("""
           SELECT r FROM Report r
           WHERE (:categoryId IS NULL OR r.category.id = :categoryId)
             AND (:status IS NULL OR r.status = :status)
             AND (:minLat IS NULL OR r.latitude >= :minLat)
             AND (:maxLat IS NULL OR r.latitude <= :maxLat)
             AND (:minLng IS NULL OR r.longitude >= :minLng)
             AND (:maxLng IS NULL OR r.longitude <= :maxLng)
             AND (:restrictToPublic = false OR r.status NOT IN :hiddenStatuses)
           ORDER BY r.createdAt DESC
           """)
    List<Report> findForMap(Long categoryId,
                             ReportStatus status,
                             BigDecimal minLat,
                             BigDecimal maxLat,
                             BigDecimal minLng,
                             BigDecimal maxLng,
                             boolean restrictToPublic,
                             List<ReportStatus> hiddenStatuses);

    // departmentId=null aggregates across every department (admin, dashboard-wide view).
    @Query("""
           SELECT COUNT(r) FROM Report r
           WHERE (:departmentId IS NULL OR r.department.id = :departmentId)
             AND r.status IN :statuses
           """)
    long countByDepartmentAndStatusIn(Long departmentId, List<ReportStatus> statuses);

    // Raw creation timestamps only — bucketed into a month-by-month series in
    // the service layer so the query stays portable across Postgres and the
    // H2 test database (no vendor-specific date-formatting SQL function).
    @Query("""
           SELECT r.createdAt FROM Report r
           WHERE (:departmentId IS NULL OR r.department.id = :departmentId)
             AND r.createdAt >= :since
           """)
    List<LocalDateTime> findCreatedAtSince(Long departmentId, LocalDateTime since);

    @Query("""
           SELECT r.department.id AS departmentId,
                  SUM(CASE WHEN r.status IN :openStatuses THEN 1 ELSE 0 END) AS openCount,
                  SUM(CASE WHEN r.status IN :resolvedStatuses THEN 1 ELSE 0 END) AS resolvedCount
           FROM Report r
           WHERE r.department IS NOT NULL
           GROUP BY r.department.id
           """)
    List<DepartmentStatusCountProjection> countOpenAndResolvedByDepartment(List<ReportStatus> openStatuses,
                                                                            List<ReportStatus> resolvedStatuses);

    // Only the two timestamps needed to compute resolution duration; averaged
    // per department in the service (Duration.between avoids DB-specific
    // interval/epoch functions).
    @Query("""
           SELECT r.department.id AS departmentId, r.approvedAt AS approvedAt, r.resolvedAt AS resolvedAt
           FROM Report r
           WHERE r.department IS NOT NULL AND r.approvedAt IS NOT NULL AND r.resolvedAt IS NOT NULL
           """)
    List<ResolutionTimeProjection> findResolutionTimes();

    @Query("""
           SELECT r.category.id AS categoryId, COUNT(r) AS total
           FROM Report r
           GROUP BY r.category.id
           """)
    List<CategoryVolumeProjection> countGroupedByCategory();
}
