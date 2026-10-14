package com.uit.scirs.report.dto;

import com.uit.scirs.feedback.dto.FeedbackDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReportDTO {

    private Long id;
    private String reportCode;
    private String title;
    private String description;
    private String status;
    private String priority;
    private Integer priorityScore;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String addressText;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private String categoryIcon;
    private Long departmentId;
    private String departmentName;
    // Only set while `department` is still null (i.e. before approval) — the
    // department the report's category auto-routes to once approved, shown to
    // the admin as "Planned department" so they can catch a miscategorized
    // report and reassign it before it goes further.
    private Long plannedDepartmentId;
    private String plannedDepartmentName;
    private Long reporterId;
    private String reporterName;
    private boolean anonymous;
    private Long assignedStaffId;
    private String assignedStaffName;
    private List<ReportImageDTO> images;
    private LocalDateTime createdAt;
    // Only set once the reporting citizen has left feedback (RESOLVED/CLOSED
    // reports only) — shown to staff/admin on the Resolution tab and to the
    // citizen on their own report detail.
    private FeedbackDTO feedback;

    private Integer aiConfidenceScore;
    private String aiTriageDecision;
    private String aiImageQuality;
    private String aiReasoning;
    private String aiFlags;
    private boolean aiTriaged;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportCode() {
        return reportCode;
    }

    public void setReportCode(String reportCode) {
        this.reportCode = reportCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getAddressText() {
        return addressText;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

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

    public Long getPlannedDepartmentId() {
        return plannedDepartmentId;
    }

    public void setPlannedDepartmentId(Long plannedDepartmentId) {
        this.plannedDepartmentId = plannedDepartmentId;
    }

    public String getPlannedDepartmentName() {
        return plannedDepartmentName;
    }

    public void setPlannedDepartmentName(String plannedDepartmentName) {
        this.plannedDepartmentName = plannedDepartmentName;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public Long getAssignedStaffId() {
        return assignedStaffId;
    }

    public void setAssignedStaffId(Long assignedStaffId) {
        this.assignedStaffId = assignedStaffId;
    }

    public String getAssignedStaffName() {
        return assignedStaffName;
    }

    public void setAssignedStaffName(String assignedStaffName) {
        this.assignedStaffName = assignedStaffName;
    }

    public List<ReportImageDTO> getImages() {
        return images;
    }

    public void setImages(List<ReportImageDTO> images) {
        this.images = images;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public FeedbackDTO getFeedback() {
        return feedback;
    }

    public void setFeedback(FeedbackDTO feedback) {
        this.feedback = feedback;
    }

    public Integer getAiConfidenceScore() {
        return aiConfidenceScore;
    }

    public void setAiConfidenceScore(Integer aiConfidenceScore) {
        this.aiConfidenceScore = aiConfidenceScore;
    }

    public String getAiTriageDecision() {
        return aiTriageDecision;
    }

    public void setAiTriageDecision(String aiTriageDecision) {
        this.aiTriageDecision = aiTriageDecision;
    }

    public String getAiImageQuality() {
        return aiImageQuality;
    }

    public void setAiImageQuality(String aiImageQuality) {
        this.aiImageQuality = aiImageQuality;
    }

    public String getAiReasoning() {
        return aiReasoning;
    }

    public void setAiReasoning(String aiReasoning) {
        this.aiReasoning = aiReasoning;
    }

    public String getAiFlags() {
        return aiFlags;
    }

    public void setAiFlags(String aiFlags) {
        this.aiFlags = aiFlags;
    }

    public boolean isAiTriaged() {
        return aiTriaged;
    }

    public void setAiTriaged(boolean aiTriaged) {
        this.aiTriaged = aiTriaged;
    }
}
