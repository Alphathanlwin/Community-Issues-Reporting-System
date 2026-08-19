package com.uit.scirs.report.service;

import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.repository.DepartmentRepository;
import com.uit.scirs.notification.service.NotificationService;
import com.uit.scirs.report.dto.CreateReportCommentDTO;
import com.uit.scirs.report.dto.ReportCommentDTO;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportComment;
import com.uit.scirs.report.mapper.ReportMapper;
import com.uit.scirs.report.repository.ReportCommentRepository;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Internal department notes on a report. Never exposed to citizens (see database-schema.md § report_comments). */
@Service
public class ReportCommentService {

    private final ReportCommentRepository reportCommentRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ReportMapper reportMapper;
    private final NotificationService notificationService;

    public ReportCommentService(ReportCommentRepository reportCommentRepository,
                                 ReportRepository reportRepository,
                                 UserRepository userRepository,
                                 DepartmentRepository departmentRepository,
                                 ReportMapper reportMapper,
                                 NotificationService notificationService) {
        this.reportCommentRepository = reportCommentRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.reportMapper = reportMapper;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ReportCommentDTO> getComments(Long reportId, CurrentUser currentUser) {
        Report report = findReport(reportId);
        assertStaffOwnsDepartment(report, currentUser);
        return reportMapper.toCommentDTOList(reportCommentRepository.findByReportIdOrderByCreatedAtAsc(reportId));
    }

    @Transactional
    public ReportCommentDTO addComment(Long reportId, CreateReportCommentDTO dto, CurrentUser currentUser) {
        Report report = findReport(reportId);
        assertStaffOwnsDepartment(report, currentUser);

        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ReportComment comment = new ReportComment();
        comment.setReport(report);
        comment.setAuthor(author);
        comment.setBody(dto.getBody());

        if (dto.getMentionedDepartmentId() != null) {
            Department mentioned = departmentRepository.findById(dto.getMentionedDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found with id: " + dto.getMentionedDepartmentId()));
            comment.setMentionedDepartment(mentioned);
        }

        ReportComment saved = reportCommentRepository.save(comment);

        if (saved.getMentionedDepartment() != null) {
            notificationService.notifyDepartmentMention(report, saved.getMentionedDepartment(), author);
        }

        return reportMapper.toCommentDTO(saved);
    }

    private void assertStaffOwnsDepartment(Report report, CurrentUser currentUser) {
        if (currentUser.getRole() == RoleName.STAFF) {
            Long staffDepartmentId = currentUser.getDepartmentId();
            Long reportDepartmentId = report.getDepartment() != null ? report.getDepartment().getId() : null;
            if (staffDepartmentId == null || !staffDepartmentId.equals(reportDepartmentId)) {
                throw new AccessDeniedException("This report belongs to another department.");
            }
        }
    }

    private Report findReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
    }
}
