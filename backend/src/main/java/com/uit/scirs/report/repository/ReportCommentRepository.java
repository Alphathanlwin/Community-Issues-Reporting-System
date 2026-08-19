package com.uit.scirs.report.repository;

import com.uit.scirs.report.entity.ReportComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {

    List<ReportComment> findByReportIdOrderByCreatedAtAsc(Long reportId);
}
