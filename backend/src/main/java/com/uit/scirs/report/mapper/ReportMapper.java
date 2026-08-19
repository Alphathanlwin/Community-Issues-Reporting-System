package com.uit.scirs.report.mapper;

import com.uit.scirs.report.dto.CreateReportDTO;
import com.uit.scirs.report.dto.ReportCommentDTO;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.dto.ReportImageDTO;
import com.uit.scirs.report.dto.ReportMapDTO;
import com.uit.scirs.report.dto.ReportStatusHistoryDTO;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportComment;
import com.uit.scirs.report.entity.ReportImage;
import com.uit.scirs.report.entity.ReportStatusHistory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReportMapper {

    public Report toEntity(CreateReportDTO dto) {
        Report entity = new Report();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setAddressText(dto.getAddressText());
        return entity;
    }

    public ReportDTO toDTO(Report entity) {
        ReportDTO dto = new ReportDTO();
        dto.setId(entity.getId());
        dto.setReportCode(entity.getReportCode());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus().name());
        dto.setPriority(entity.getPriority().name());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setAddressText(entity.getAddressText());

        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
        }

        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartmentName(entity.getDepartment().getName());
        }

        if (entity.getReporter() != null) {
            dto.setReporterId(entity.getReporter().getId());
            dto.setReporterName(entity.getReporter().getFullName());
        }

        if (entity.getAssignedStaff() != null) {
            dto.setAssignedStaffId(entity.getAssignedStaff().getId());
            dto.setAssignedStaffName(entity.getAssignedStaff().getFullName());
        }

        dto.setImages(entity.getImages().stream().map(this::toImageDTO).toList());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public List<ReportDTO> toDTOList(List<Report> entities) {
        return entities.stream().map(this::toDTO).toList();
    }

    public ReportStatusHistoryDTO toHistoryDTO(ReportStatusHistory entity) {
        ReportStatusHistoryDTO dto = new ReportStatusHistoryDTO();
        dto.setId(entity.getId());
        dto.setOldStatus(entity.getOldStatus() != null ? entity.getOldStatus().name() : null);
        dto.setNewStatus(entity.getNewStatus().name());
        if (entity.getChangedBy() != null) {
            dto.setChangedById(entity.getChangedBy().getId());
            dto.setChangedByName(entity.getChangedBy().getFullName());
        }
        dto.setRemarks(entity.getRemarks());
        dto.setChangedAt(entity.getChangedAt());
        return dto;
    }

    public List<ReportStatusHistoryDTO> toHistoryDTOList(List<ReportStatusHistory> entities) {
        return entities.stream().map(this::toHistoryDTO).toList();
    }

    public ReportMapDTO toMapDTO(Report entity) {
        ReportMapDTO dto = new ReportMapDTO();
        dto.setId(entity.getId());
        dto.setReportCode(entity.getReportCode());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setStatus(entity.getStatus().name());
        dto.setPriority(entity.getPriority().name());
        if (entity.getCategory() != null) {
            dto.setCategoryName(entity.getCategory().getName());
            dto.setCategoryColor(entity.getCategory().getColorHex());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public List<ReportMapDTO> toMapDTOList(List<Report> entities) {
        return entities.stream().map(this::toMapDTO).toList();
    }

    public ReportCommentDTO toCommentDTO(ReportComment entity) {
        ReportCommentDTO dto = new ReportCommentDTO();
        dto.setId(entity.getId());
        dto.setReportId(entity.getReport().getId());
        dto.setAuthorId(entity.getAuthor().getId());
        dto.setAuthorName(entity.getAuthor().getFullName());
        dto.setBody(entity.getBody());
        if (entity.getMentionedDepartment() != null) {
            dto.setMentionedDepartmentId(entity.getMentionedDepartment().getId());
            dto.setMentionedDepartmentName(entity.getMentionedDepartment().getName());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public List<ReportCommentDTO> toCommentDTOList(List<ReportComment> entities) {
        return entities.stream().map(this::toCommentDTO).toList();
    }

    private ReportImageDTO toImageDTO(ReportImage entity) {
        ReportImageDTO dto = new ReportImageDTO();
        dto.setId(entity.getId());
        dto.setImageUrl(entity.getImageUrl());
        dto.setImageType(entity.getImageType().name());
        dto.setUploadedAt(entity.getUploadedAt());
        return dto;
    }
}
