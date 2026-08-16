package com.uit.scirs.report.dto;

import jakarta.validation.constraints.NotBlank;

public class RejectReportDTO {

    @NotBlank(message = "A rejection reason is required")
    private String rejectionReason;

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
