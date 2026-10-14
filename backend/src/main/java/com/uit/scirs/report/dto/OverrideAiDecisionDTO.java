package com.uit.scirs.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload submitted by an Administrator when overriding an AI auto-triage decision.
 */
public class OverrideAiDecisionDTO {

    @NotNull(message = "newDepartmentId is required")
    private Long newDepartmentId;

    @NotBlank(message = "Override reason is required for administrative accountability")
    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;

    public OverrideAiDecisionDTO() {
    }

    public OverrideAiDecisionDTO(Long newDepartmentId, String reason) {
        this.newDepartmentId = newDepartmentId;
        this.reason = reason;
    }

    public Long getNewDepartmentId() {
        return newDepartmentId;
    }

    public void setNewDepartmentId(Long newDepartmentId) {
        this.newDepartmentId = newDepartmentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
