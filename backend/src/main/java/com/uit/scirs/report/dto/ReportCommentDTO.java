package com.uit.scirs.report.dto;

import java.time.LocalDateTime;

public class ReportCommentDTO {

    private Long id;
    private Long reportId;
    private Long authorId;
    private String authorName;
    private String body;
    private Long mentionedDepartmentId;
    private String mentionedDepartmentName;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Long getMentionedDepartmentId() {
        return mentionedDepartmentId;
    }

    public void setMentionedDepartmentId(Long mentionedDepartmentId) {
        this.mentionedDepartmentId = mentionedDepartmentId;
    }

    public String getMentionedDepartmentName() {
        return mentionedDepartmentName;
    }

    public void setMentionedDepartmentName(String mentionedDepartmentName) {
        this.mentionedDepartmentName = mentionedDepartmentName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
