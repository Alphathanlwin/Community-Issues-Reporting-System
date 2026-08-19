package com.uit.scirs.report.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateReportPriorityDTO {

    @NotBlank(message = "Priority is required")
    private String priority;

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
