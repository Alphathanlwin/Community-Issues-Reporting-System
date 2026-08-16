package com.uit.scirs.notification.service;

import com.uit.scirs.common.integration.EmailService;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.notification.dto.NotificationDTO;
import com.uit.scirs.notification.entity.Notification;
import com.uit.scirs.notification.entity.NotificationType;
import com.uit.scirs.notification.mapper.NotificationMapper;
import com.uit.scirs.notification.repository.NotificationRepository;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportStatus;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationMapper notificationMapper;
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    @InjectMocks NotificationService notificationService;

    @Test
    void notifyNewReport_createsOneNotificationPerAdmin() {
        Report report = report();
        User admin1 = user(1L, "admin1@scirs.gov");
        User admin2 = user(2L, "admin2@scirs.gov");
        when(userRepository.findByRoleName(RoleName.ADMIN)).thenReturn(List.of(admin1, admin2));

        notificationService.notifyNewReport(report);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getType)
                .containsOnly(NotificationType.NEW_REPORT);
        verify(emailService).send(eq("admin1@scirs.gov"), anyString(), anyString());
        verify(emailService).send(eq("admin2@scirs.gov"), anyString(), anyString());
    }

    @Test
    void notifyStatusChange_whenResolved_notifiesReporterWithCompletedType() {
        Report report = report();
        report.setStatus(ReportStatus.RESOLVED);

        notificationService.notifyStatusChange(report);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.REPORT_COMPLETED);
        assertThat(captor.getValue().getRecipient()).isEqualTo(report.getReporter());
    }

    @Test
    void markRead_whenNotOwner_throwsAccessDeniedException() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setRecipient(user(7L, "citizen@example.com"));

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markRead(1L, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void markRead_whenOwner_marksReadAndReturnsDTO() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setRecipient(user(7L, "citizen@example.com"));

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        when(notificationMapper.toDTO(any(Notification.class))).thenReturn(new NotificationDTO());

        notificationService.markRead(1L, 7L);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void unreadCount_delegatesToRepository() {
        when(notificationRepository.countByRecipientIdAndReadFalse(7L)).thenReturn(3L);

        assertThat(notificationService.unreadCount(7L)).isEqualTo(3L);
    }

    private Report report() {
        Report report = new Report();
        report.setId(1L);
        report.setReportCode("RPT-2026-000001");
        report.setTitle("Pothole");
        report.setStatus(ReportStatus.PENDING_APPROVAL);
        report.setReporter(user(7L, "citizen@example.com"));
        Department department = new Department();
        department.setId(2L);
        report.setDepartment(department);
        return report;
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName("Test User");
        return user;
    }
}
