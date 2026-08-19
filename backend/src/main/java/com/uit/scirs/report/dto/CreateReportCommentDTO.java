package com.uit.scirs.report.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateReportCommentDTO {

    @NotBlank(message = "Comment body is required")
    private String body;

    private Long mentionedDepartmentId;

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
}
