package com.uit.scirs.notification.service;

import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.integration.EmailService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                                NotificationMapper notificationMapper,
                                UserRepository userRepository,
                                EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void notifyNewReport(Report report) {
        String title = "New report awaiting approval";
        String message = "Report " + report.getReportCode() + " (" + report.getTitle() + ") needs approval.";
        userRepository.findByRoleName(RoleName.ADMIN)
                .forEach(admin -> create(admin, report, NotificationType.NEW_REPORT, title, message));
    }

    @Transactional
    public void notifyStatusChange(Report report) {
        create(report.getReporter(), report, typeFor(report.getStatus()), titleFor(report.getStatus()),
                "Your report " + report.getReportCode() + " is now " + humanize(report.getStatus()) + ".");

        if (report.getStatus() == ReportStatus.ASSIGNED) {
            notifyDepartmentStaff(report, NotificationType.NEW_REPORT, "New report assigned to your department",
                    "Report " + report.getReportCode() + " was routed to your department.");
        }
    }

    @Transactional
    public void notifyAccountApproved(User user) {
        create(user, null, NotificationType.ACCOUNT_APPROVED, "Account approved",
                "Your account has been approved. You can now sign in.");
    }

    @Transactional
    public void notifyAccountRejected(User user, String reason) {
        create(user, null, NotificationType.ACCOUNT_REJECTED, "Account registration rejected",
                "Your account registration was rejected" + (reason != null && !reason.isBlank() ? ": " + reason : "."));
    }

    @Transactional
    public void sweepWaitingTooLong(List<Report> staleReports) {
        for (Report report : staleReports) {
            boolean alreadyAlerted = notificationRepository.existsByReportIdAndTypeAndCreatedAtAfter(
                    report.getId(), NotificationType.REPORT_WAITING_TOO_LONG, report.getUpdatedAt());
            if (!alreadyAlerted) {
                notifyDepartmentStaff(report, NotificationType.REPORT_WAITING_TOO_LONG, "Report waiting too long",
                        "Report " + report.getReportCode() + " has been " + humanize(report.getStatus())
                                + " for longer than expected.");
            }
        }
    }

    private void notifyDepartmentStaff(Report report, NotificationType type, String title, String message) {
        if (report.getDepartment() == null) {
            return;
        }
        userRepository.findByDepartmentId(report.getDepartment().getId())
                .forEach(staff -> create(staff, report, type, title, message));
    }

    private void create(User recipient, Report report, NotificationType type, String title, String message) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setReport(report);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);
        emailService.send(recipient.getEmail(), title, message);
    }

    private NotificationType typeFor(ReportStatus status) {
        return switch (status) {
            case ASSIGNED -> NotificationType.REPORT_APPROVED;
            case REJECTED -> NotificationType.REPORT_REJECTED;
            case RESOLVED -> NotificationType.REPORT_COMPLETED;
            default -> NotificationType.STATUS_CHANGED;
        };
    }

    private String titleFor(ReportStatus status) {
        return switch (status) {
            case PENDING_APPROVAL -> "Report submitted";
            case ASSIGNED -> "Report approved";
            case REJECTED -> "Report denied";
            case IN_PROGRESS -> "Report in progress";
            case RESOLVED -> "Report resolved";
            case CLOSED -> "Report closed";
        };
    }

    private String humanize(ReportStatus status) {
        return status.name().replace('_', ' ').toLowerCase();
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> list(Long recipientId, boolean unreadOnly) {
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        return notificationMapper.toDTOList(notifications);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Transactional
    public NotificationDTO markRead(Long notificationId, Long currentUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipient().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only manage your own notifications.");
        }

        notification.setRead(true);
        return notificationMapper.toDTO(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(Long recipientId) {
        notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId)
                .forEach(notification -> notification.setRead(true));
    }
}
