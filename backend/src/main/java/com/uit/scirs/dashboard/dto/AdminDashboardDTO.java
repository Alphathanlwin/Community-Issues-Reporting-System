package com.uit.scirs.dashboard.dto;

import com.uit.scirs.auth.dto.UserDTO;
import com.uit.scirs.report.dto.ReportDTO;

import java.util.List;

public class AdminDashboardDTO {

    private long pendingAccountCount;
    private long pendingReportCount;
    private List<UserDTO> recentRegistrations;
    private List<ReportDTO> reportsAwaitingApproval;

    public long getPendingAccountCount() {
        return pendingAccountCount;
    }

    public void setPendingAccountCount(long pendingAccountCount) {
        this.pendingAccountCount = pendingAccountCount;
    }

    public long getPendingReportCount() {
        return pendingReportCount;
    }

    public void setPendingReportCount(long pendingReportCount) {
        this.pendingReportCount = pendingReportCount;
    }

    public List<UserDTO> getRecentRegistrations() {
        return recentRegistrations;
    }

    public void setRecentRegistrations(List<UserDTO> recentRegistrations) {
        this.recentRegistrations = recentRegistrations;
    }

    public List<ReportDTO> getReportsAwaitingApproval() {
        return reportsAwaitingApproval;
    }

    public void setReportsAwaitingApproval(List<ReportDTO> reportsAwaitingApproval) {
        this.reportsAwaitingApproval = reportsAwaitingApproval;
    }
}
