package com.uit.scirs.feedback.mapper;

import com.uit.scirs.feedback.dto.FeedbackDTO;
import com.uit.scirs.feedback.entity.Feedback;
import org.springframework.stereotype.Component;

@Component
public class FeedbackMapper {

    public FeedbackDTO toDTO(Feedback entity) {
        FeedbackDTO dto = new FeedbackDTO();
        dto.setId(entity.getId());
        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        if (entity.getReport() != null) {
            dto.setReportId(entity.getReport().getId());
            dto.setReportCode(entity.getReport().getReportCode());
        }
        if (entity.getCitizen() != null) {
            dto.setCitizenId(entity.getCitizen().getId());
            dto.setCitizenName(entity.getCitizen().getFullName());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
