package com.uit.scirs.notification.mapper;

import com.uit.scirs.notification.dto.NotificationDTO;
import com.uit.scirs.notification.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationMapper {

    public NotificationDTO toDTO(Notification entity) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(entity.getId());
        dto.setType(entity.getType().name());
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setRead(entity.isRead());
        if (entity.getReport() != null) {
            dto.setReportId(entity.getReport().getId());
            dto.setReportCode(entity.getReport().getReportCode());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public List<NotificationDTO> toDTOList(List<Notification> entities) {
        return entities.stream().map(this::toDTO).toList();
    }
}
