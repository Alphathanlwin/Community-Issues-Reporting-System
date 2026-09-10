package com.uit.scirs.report.dto;

/**
 * Returned by {@code POST /api/reports/{id}/support}: the report's new support
 * tally, the points this action awarded the supporter, and the supporter's
 * updated running total (so the client can refresh the score/leaderboard
 * without a second round trip).
 */
public class ReportSupportResultDTO {

    private long supportCount;
    private int awardedPoints;
    private int totalPoints;
    private int remainingToday;

    public ReportSupportResultDTO() {
    }

    public ReportSupportResultDTO(long supportCount, int awardedPoints, int totalPoints, int remainingToday) {
        this.supportCount = supportCount;
        this.awardedPoints = awardedPoints;
        this.totalPoints = totalPoints;
        this.remainingToday = remainingToday;
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

    public int getRemainingToday() {
        return remainingToday;
    }

    public void setRemainingToday(int remainingToday) {
        this.remainingToday = remainingToday;
    }
}
