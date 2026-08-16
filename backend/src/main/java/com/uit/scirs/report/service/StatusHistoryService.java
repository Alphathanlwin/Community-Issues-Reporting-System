package com.uit.scirs.report.service;

import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.entity.ReportStatusHistory;
import com.uit.scirs.report.repository.ReportStatusHistoryRepository;
import com.uit.scirs.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusHistoryService {

    private final ReportStatusHistoryRepository reportStatusHistoryRepository;

    public StatusHistoryService(ReportStatusHistoryRepository reportStatusHistoryRepository) {
        this.reportStatusHistoryRepository = reportStatusHistoryRepository;
    }

    @Transactional
    public void record(Report report, ReportStatus oldStatus, ReportStatus newStatus, User changedBy, String remarks) {
        ReportStatusHistory history = new ReportStatusHistory();
        history.setReport(report);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setRemarks(remarks);
        reportStatusHistoryRepository.save(history);
    }
}
