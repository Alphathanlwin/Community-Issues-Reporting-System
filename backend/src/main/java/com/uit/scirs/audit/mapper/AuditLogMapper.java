package com.uit.scirs.audit.mapper;

import com.uit.scirs.audit.dto.AuditLogDTO;
import com.uit.scirs.audit.entity.AuditLog;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuditLogMapper {

    public AuditLogDTO toDTO(AuditLog entity) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(entity.getId());
        dto.setTimestamp(entity.getCreatedAt());
        dto.setActorId(entity.getActorId());
        dto.setActorName(entity.getActorName());
        dto.setActorEmail(entity.getActorEmail());
        dto.setAction(entity.getAction().name());
        dto.setTargetType(entity.getTargetType());
        dto.setTargetId(entity.getTargetId());
        dto.setTargetLabel(entity.getTargetLabel());
        dto.setDetails(entity.getDetails());
        return dto;
    }

    public List<AuditLogDTO> toDTOList(List<AuditLog> entities) {
        return entities.stream().map(this::toDTO).toList();
    }
}
