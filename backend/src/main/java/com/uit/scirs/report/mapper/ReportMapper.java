package com.uit.scirs.report.mapper;

import com.uit.scirs.feedback.entity.Feedback;
import com.uit.scirs.feedback.mapper.FeedbackMapper;
import com.uit.scirs.feedback.repository.FeedbackRepository;
import com.uit.scirs.report.dto.CreateReportDTO;
import com.uit.scirs.report.dto.PublicReportDTO;
import com.uit.scirs.report.dto.ReportCommentDTO;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.dto.ReportImageDTO;
import com.uit.scirs.report.dto.ReportMapDTO;
import com.uit.scirs.report.dto.ReportStatusHistoryDTO;
import com.uit.scirs.report.entity.ImageType;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportComment;
import com.uit.scirs.report.entity.ReportImage;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.entity.ReportStatusHistory;
import com.uit.scirs.report.repository.ReportSupportRepository;
import com.uit.scirs.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class ReportMapper {

    // Feedback can only exist once a report reaches one of these — skip the
    // lookup for every other report instead of querying feedback rows that
    // could never exist.
    private static final Set<ReportStatus> FEEDBACK_ELIGIBLE_STATUSES =
            EnumSet.of(ReportStatus.RESOLVED, ReportStatus.CLOSED);

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;
    private final ReportSupportRepository reportSupportRepository;

    public ReportMapper(FeedbackRepository feedbackRepository, FeedbackMapper feedbackMapper,
                         ReportSupportRepository reportSupportRepository) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackMapper = feedbackMapper;
        this.reportSupportRepository = reportSupportRepository;
    }

    public Report toEntity(CreateReportDTO dto) {
        Report entity = new Report();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setAddressText(dto.getAddressText());
        entity.setAnonymous(Boolean.TRUE.equals(dto.getIsAnonymous()));
        return entity;
    }

    public ReportDTO toDTO(Report entity) {
        Feedback feedback = null;
        if (FEEDBACK_ELIGIBLE_STATUSES.contains(entity.getStatus())) {
            feedback = feedbackRepository.findByReportId(entity.getId()).orElse(null);
        }
        return toDTO(entity, feedback);
    }

    public ReportDTO toDTO(Report entity, Feedback feedback) {
        ReportDTO dto = new ReportDTO();
        dto.setId(entity.getId());
        dto.setReportCode(entity.getReportCode());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus().name());
        dto.setPriority(entity.getPriority().name());
        dto.setPriorityScore(entity.getPriorityScore());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setAddressText(entity.getAddressText());

        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
            dto.setCategoryColor(entity.getCategory().getColorHex());
            dto.setCategoryIcon(entity.getCategory().getIcon());
        }

        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartmentName(entity.getDepartment().getName());
        } else if (entity.getCategory() != null && entity.getCategory().getDepartment() != null) {
            dto.setPlannedDepartmentId(entity.getCategory().getDepartment().getId());
            dto.setPlannedDepartmentName(entity.getCategory().getDepartment().getName());
        }

        // ADMIN / STAFF DTO — identity is always retained for accountability,
        // with `anonymous` flagging how the report appears to the public.
        dto.setAnonymous(entity.isAnonymous());
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

        if (feedback != null) {
            dto.setFeedback(feedbackMapper.toDTO(feedback));
        }

        return dto;
    }

    public List<ReportDTO> toDTOList(List<Report> entities) {
        if (entities == null || entities.isEmpty()) {
            return java.util.List.of();
        }
        List<Long> eligibleIds = entities.stream()
                .filter(r -> FEEDBACK_ELIGIBLE_STATUSES.contains(r.getStatus()))
                .map(Report::getId)
                .toList();
        java.util.Map<Long, Feedback> feedbackMap = eligibleIds.isEmpty() ? java.util.Map.of() :
                feedbackRepository.findByReportIdIn(eligibleIds).stream()
                        .collect(java.util.stream.Collectors.toMap(f -> f.getReport().getId(), f -> f, (a, b) -> a));

        return entities.stream().map(r -> toDTO(r, feedbackMap.get(r.getId()))).toList();
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
        return toMapDTO(entity, reportSupportRepository.countByReportId(entity.getId()));
    }

    public ReportMapDTO toMapDTO(Report entity, long supportCount) {
        ReportMapDTO dto = new ReportMapDTO();
        dto.setId(entity.getId());
        dto.setReportCode(entity.getReportCode());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setStatus(entity.getStatus().name());
        dto.setPriority(entity.getPriority().name());
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
            dto.setCategoryColor(entity.getCategory().getColorHex());
        }
        // Pseudo-anonymous: the reporter's name is stripped for anonymous reports
        // (the same masking the public feed applies); reporter_id stays server-side.
        dto.setAnonymous(entity.isAnonymous());
        if (!entity.isAnonymous() && entity.getReporter() != null) {
            dto.setReporterName(entity.getReporter().getFullName());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setSupportCount(supportCount);
        return dto;
    }

    public List<ReportMapDTO> toMapDTOList(List<Report> entities) {
        if (entities == null || entities.isEmpty()) {
            return java.util.List.of();
        }
        List<Long> ids = entities.stream().map(Report::getId).toList();
        java.util.Map<Long, Long> supportCounts = reportSupportRepository.countByReportIdIn(ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.uit.scirs.report.repository.ReportSupportCountProjection::getReportId,
                        com.uit.scirs.report.repository.ReportSupportCountProjection::getCount));

        return entities.stream().map(r -> toMapDTO(r, supportCounts.getOrDefault(r.getId(), 0L))).toList();
    }

    /**
     * Citizen public-feed projection. The reporter's identity is stripped here
     * — before serialization — for anonymous reports; it is never sent to a
     * public caller.
     */
    public PublicReportDTO toPublicDTO(Report entity) {
        PublicReportDTO dto = new PublicReportDTO();
        dto.setId(entity.getId());
        dto.setReportCode(entity.getReportCode());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus().name());
        dto.setPriority(entity.getPriority().name());
        dto.setPriorityScore(entity.getPriorityScore());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setAddressText(entity.getAddressText());
        dto.setCreatedAt(entity.getCreatedAt());

        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
            dto.setCategoryColor(entity.getCategory().getColorHex());
        }

        dto.setImageUrl(entity.getImages().stream()
                .filter(image -> image.getImageType() == ImageType.REPORT_PHOTO)
                .map(ReportImage::getImageUrl)
                .findFirst()
                .orElse(null));

        dto.setAnonymous(entity.isAnonymous());
        User reporter = entity.getReporter();
        if (!entity.isAnonymous() && reporter != null) {
            dto.setReporterName(reporter.getFullName());
            dto.setReporterAvatarUrl(reporter.getProfileImageUrl());
        }
        dto.setSupportCount(reportSupportRepository.countByReportId(entity.getId()));
        return dto;
    }

    public List<PublicReportDTO> toPublicDTOList(List<Report> entities) {
        return entities.stream().map(this::toPublicDTO).toList();
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
