package com.uit.scirs.feedback.repository;

import com.uit.scirs.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByReportId(Long reportId);

    boolean existsByReportId(Long reportId);

    List<Feedback> findByCitizenId(Long citizenId);

    @Query("""
           SELECT f.report.department.id AS departmentId, AVG(f.rating) AS averageRating
           FROM Feedback f
           WHERE f.report.department IS NOT NULL
           GROUP BY f.report.department.id
           """)
    List<DepartmentRatingProjection> averageRatingByDepartment();
}
