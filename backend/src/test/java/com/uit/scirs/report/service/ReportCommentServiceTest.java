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
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.mapper.ReportMapper;
import com.uit.scirs.report.repository.ReportCommentRepository;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportCommentServiceTest {

    @Mock ReportCommentRepository reportCommentRepository;
    @Mock ReportRepository reportRepository;
    @Mock UserRepository userRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock ReportMapper reportMapper;
    @Mock NotificationService notificationService;
    @InjectMocks ReportCommentService commentService;

    @Test
    void addComment_withoutMention_savesCommentAndDoesNotNotify() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, roads);
        User author = user(50L, "Thida Win");

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(50L)).thenReturn(Optional.of(author));
        when(reportCommentRepository.save(any(ReportComment.class))).thenAnswer(i -> i.getArgument(0));
        when(reportMapper.toCommentDTO(any(ReportComment.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        CreateReportCommentDTO dto = new CreateReportCommentDTO();
        dto.setBody("Crew scheduled for tomorrow.");

        ReportCommentDTO result = commentService.addComment(10L, dto, currentUser(50L, RoleName.STAFF, 2L));

        assertThat(result.getBody()).isEqualTo("Crew scheduled for tomorrow.");
        verify(notificationService, never()).notifyDepartmentMention(any(), any(), any());
    }

    @Test
    void addComment_withMentionedDepartment_notifiesThatDepartment() {
        Department roads = department(2L, "Roads");
        Department water = department(3L, "Water");
        Report report = report(10L, roads);
        User author = user(50L, "Thida Win");

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(50L)).thenReturn(Optional.of(author));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(water));
        when(reportCommentRepository.save(any(ReportComment.class))).thenAnswer(i -> i.getArgument(0));
        when(reportMapper.toCommentDTO(any(ReportComment.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        CreateReportCommentDTO dto = new CreateReportCommentDTO();
        dto.setBody("Need Water dept to shut off the main.");
        dto.setMentionedDepartmentId(3L);

        commentService.addComment(10L, dto, currentUser(50L, RoleName.STAFF, 2L));

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(notificationService).notifyDepartmentMention(any(Report.class), captor.capture(), any(User.class));
        assertThat(captor.getValue().getId()).isEqualTo(3L);
    }

    @Test
    void getComments_whenStaffFromAnotherDepartment_throwsAccessDeniedException() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, roads);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> commentService.getComments(10L, currentUser(50L, RoleName.STAFF, 3L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getComments_whenAdmin_returnsCommentsRegardlessOfDepartment() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, roads);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(reportCommentRepository.findByReportIdOrderByCreatedAtAsc(10L)).thenReturn(java.util.List.of());
        when(reportMapper.toCommentDTOList(java.util.List.of())).thenReturn(java.util.List.of());

        commentService.getComments(10L, currentUser(1L, RoleName.ADMIN, null));

        verify(reportCommentRepository).findByReportIdOrderByCreatedAtAsc(10L);
    }

    @Test
    void addComment_withUnknownMentionedDepartment_throwsResourceNotFoundException() {
        Department roads = department(2L, "Roads");
        Report report = report(10L, roads);
        User author = user(50L, "Thida Win");

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(50L)).thenReturn(Optional.of(author));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        CreateReportCommentDTO dto = new CreateReportCommentDTO();
        dto.setBody("Mentioning a department that doesn't exist.");
        dto.setMentionedDepartmentId(999L);

        assertThatThrownBy(() -> commentService.addComment(10L, dto, currentUser(50L, RoleName.STAFF, 2L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reportCommentRepository, never()).save(any(ReportComment.class));
    }

    private Department department(Long id, String name) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setActive(true);
        return department;
    }

    private Report report(Long id, Department department) {
        Report report = new Report();
        report.setId(id);
        report.setDepartment(department);
        report.setStatus(ReportStatus.ASSIGNED);
        return report;
    }

    private User user(Long id, String fullName) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        return user;
    }

    private CurrentUser currentUser(Long id, RoleName role, Long departmentId) {
        return new CurrentUser(id, "user@example.com", role, departmentId);
    }

    private ReportCommentDTO dtoFor(ReportComment entity) {
        ReportCommentDTO dto = new ReportCommentDTO();
        dto.setId(entity.getId());
        dto.setBody(entity.getBody());
        if (entity.getMentionedDepartment() != null) {
            dto.setMentionedDepartmentId(entity.getMentionedDepartment().getId());
        }
        return dto;
    }
}
