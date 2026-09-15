package com.uit.scirs.report.dto;

/** Response for POST/DELETE /api/reports/{id}/support — matches the frontend's communitySupportApi.js contract. */
public class ReportSupportResultDTO {

    private Long reportId;
    private long supportCount;
    private int awardedPoints;
    private int totalPoints;
    private long remainingToday;

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public long getSupportCount() {
        return supportCount;
    }

    public void setSupportCount(long supportCount) {
        this.supportCount = supportCount;
    }

    public int getAwardedPoints() {
        return awardedPoints;
    }

    public void setAwardedPoints(int awardedPoints) {
        this.awardedPoints = awardedPoints;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public long getRemainingToday() {
        return remainingToday;
    }

    public void setRemainingToday(long remainingToday) {
        this.remainingToday = remainingToday;
    }
}
