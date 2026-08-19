package com.uit.scirs.dashboard.dto;

import com.uit.scirs.report.dto.ReportDTO;

import java.util.List;

public class StaffDashboardDTO {

    private long totalReports;
    private long resolvedReports;
    private long remainingReports;
    private long newReports;
    private List<MonthlyReportCountDTO> monthlySeries;
    private List<ReportDTO> recentReports;

    public long getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(long totalReports) {
        this.totalReports = totalReports;
    }

    public long getResolvedReports() {
        return resolvedReports;
    }

    public void setResolvedReports(long resolvedReports) {
        this.resolvedReports = resolvedReports;
    }

    public long getRemainingReports() {
        return remainingReports;
    }

    public void setRemainingReports(long remainingReports) {
        this.remainingReports = remainingReports;
    }

    public long getNewReports() {
        return newReports;
    }

    public void setNewReports(long newReports) {
        this.newReports = newReports;
    }

    public List<MonthlyReportCountDTO> getMonthlySeries() {
        return monthlySeries;
    }

    public void setMonthlySeries(List<MonthlyReportCountDTO> monthlySeries) {
        this.monthlySeries = monthlySeries;
    }

    public List<ReportDTO> getRecentReports() {
        return recentReports;
    }

    public void setRecentReports(List<ReportDTO> recentReports) {
        this.recentReports = recentReports;
    }
}
