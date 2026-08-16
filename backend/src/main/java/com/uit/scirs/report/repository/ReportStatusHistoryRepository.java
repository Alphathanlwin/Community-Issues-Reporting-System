package com.uit.scirs.report.repository;

import com.uit.scirs.report.entity.ReportStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportStatusHistoryRepository extends JpaRepository<ReportStatusHistory, Long> {

    List<ReportStatusHistory> findByReportIdOrderByChangedAtAsc(Long reportId);
}
