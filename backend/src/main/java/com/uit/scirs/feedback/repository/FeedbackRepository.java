package com.uit.scirs.feedback.repository;

import com.uit.scirs.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByReportId(Long reportId);

    boolean existsByReportId(Long reportId);

    List<Feedback> findByCitizenId(Long citizenId);
}
