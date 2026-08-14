package com.uit.scirs.report.mapper;

import com.uit.scirs.report.dto.CreateReportDTO;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.dto.ReportImageDTO;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportImage;
import org.springframework.stereotype.Component;

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

        dto.setImages(entity.getImages().stream().map(this::toImageDTO).toList());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
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
