package com.uit.scirs.dashboard.dto;

public class DepartmentPerformanceDTO {

    private Long departmentId;
    private String departmentName;
    private long openCount;
    private long resolvedCount;
    private Double averageResolutionHours;
    private Double averageRating;

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public long getOpenCount() {
        return openCount;
    }

    public void setOpenCount(long openCount) {
        this.openCount = openCount;
    }

    public long getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }

    public Double getAverageResolutionHours() {
        return averageResolutionHours;
    }

    public void setAverageResolutionHours(Double averageResolutionHours) {
        this.averageResolutionHours = averageResolutionHours;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }
}
