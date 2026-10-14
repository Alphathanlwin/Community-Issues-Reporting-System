package com.uit.scirs.report.dto;

import com.uit.scirs.report.entity.AiImageQuality;
import com.uit.scirs.report.entity.AiTriageDecision;

import java.util.ArrayList;
import java.util.List;

/**
 * Result produced by the AI Image Analysis & Triage Engine.
 */
public class AiTriageResultDTO {

    private Integer confidenceScore;
    private AiTriageDecision decision;
    private AiImageQuality imageQuality;
    private String reasoning;
    private List<String> flags = new ArrayList<>();
    private Long suggestedDepartmentId;
    private String suggestedDepartmentName;

    public AiTriageResultDTO() {
    }

    public static AiTriageResultDTO autoRouted(int confidenceScore, Long departmentId, String departmentName,
                                              String reasoning, List<String> flags) {
        AiTriageResultDTO dto = new AiTriageResultDTO();
        dto.setConfidenceScore(confidenceScore);
        dto.setDecision(AiTriageDecision.AUTO_ROUTED);
        dto.setImageQuality(AiImageQuality.CLEAR);
        dto.setSuggestedDepartmentId(departmentId);
        dto.setSuggestedDepartmentName(departmentName);
        dto.setReasoning(reasoning);
        if (flags != null) {
            dto.setFlags(flags);
        }
        return dto;
    }

    public static AiTriageResultDTO flagged(int confidenceScore, AiImageQuality quality, String reasoning,
                                           List<String> flags) {
        AiTriageResultDTO dto = new AiTriageResultDTO();
        dto.setConfidenceScore(confidenceScore);
        dto.setDecision(AiTriageDecision.FLAGGED_FOR_REVIEW);
        dto.setImageQuality(quality);
        dto.setReasoning(reasoning);
        if (flags != null) {
            dto.setFlags(flags);
        }
        return dto;
    }

    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public AiTriageDecision getDecision() {
        return decision;
    }

    public void setDecision(AiTriageDecision decision) {
        this.decision = decision;
    }

    public AiImageQuality getImageQuality() {
        return imageQuality;
    }

    public void setImageQuality(AiImageQuality imageQuality) {
        this.imageQuality = imageQuality;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public List<String> getFlags() {
        return flags;
    }

    public void setFlags(List<String> flags) {
        this.flags = flags;
    }

    public Long getSuggestedDepartmentId() {
        return suggestedDepartmentId;
    }

    public void setSuggestedDepartmentId(Long suggestedDepartmentId) {
        this.suggestedDepartmentId = suggestedDepartmentId;
    }

    public String getSuggestedDepartmentName() {
        return suggestedDepartmentName;
    }

    public void setSuggestedDepartmentName(String suggestedDepartmentName) {
        this.suggestedDepartmentName = suggestedDepartmentName;
    }
}
