package com.uit.scirs.notification.service;

import com.uit.scirs.common.exception.ResourceNotFoundException;
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
        String reporterName = report.isAnonymous() ? "A citizen" : report.getReporter().getFullName();
        String message = reporterName + " reported a " + report.getCategory().getName() + " issue: "
                + truncate(report.getDescription(), 80);
        userRepository.findByRoleName(RoleName.ADMIN)
                .forEach(admin -> create(admin, report, NotificationType.NEW_REPORT, title, message));
    }

    /** Truncates on a word boundary so the notification card shows "just the beginning" of a long description. */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        String cut = text.substring(0, maxLength);
        int lastSpace = cut.lastIndexOf(' ');
        return (lastSpace > 0 ? cut.substring(0, lastSpace) : cut) + "…";
    }

    /** "<category>: <description snippet>" — appended to every report notification so the card is self-explanatory without opening the report. */
    private String reportSummary(Report report) {
        return report.getCategory().getName() + ": " + truncate(report.getDescription(), 80);
    }

    @Transactional
    public void notifyStatusChange(Report report) {
        notifyStatusChange(report, null);
    }

    /**
     * Same as {@link #notifyStatusChange(Report)}, but for the citizen-feedback-closes-
     * the-report path: carries the star rating so the staff (and reporter) notification
     * cards can render it, instead of just a plain "closed" line.
     */
    @Transactional
    public void notifyStatusChange(Report report, Integer feedbackRating) {
        String message = "Your report " + report.getReportCode() + " (" + reportSummary(report) + ") is now "
                + humanize(report.getStatus()) + ".";
        if (report.getStatus() == ReportStatus.REJECTED
                && report.getRejectionReason() != null && !report.getRejectionReason().isBlank()) {
            message += " Reason: " + report.getRejectionReason();
        }
        create(report.getReporter(), report, typeFor(report.getStatus()), titleFor(report.getStatus()), message,
                feedbackRating);

        if (report.getStatus() == ReportStatus.ASSIGNED) {
            notifyDepartmentStaff(report, NotificationType.NEW_REPORT, "New report assigned to your department",
                    "Report " + report.getReportCode() + " (" + reportSummary(report) + ") was routed to your department.");
        } else if (report.getStatus() == ReportStatus.CLOSED) {
            String staffMessage = "Report " + report.getReportCode() + " (" + reportSummary(report) + ") was closed after citizen feedback."
                    + (feedbackRating != null ? " Rating: " + feedbackRating + "/5." : "");
            notifyDepartmentStaff(report, NotificationType.STATUS_CHANGED, "Report closed with citizen feedback",
                    staffMessage, feedbackRating);
        }
    }

    /**
     * The citizen left feedback on a report that was already CLOSED (staff closed it
     * directly, ahead of the feedback) — status doesn't change so notifyStatusChange
     * never fires here, but the department still needs to hear about the rating.
     */
    @Transactional
    public void notifyFeedbackReceived(Report report, int rating, String comment) {
        String title = "New feedback received";
        String message = "Report " + report.getReportCode() + " (" + reportSummary(report) + ") received "
                + rating + "/5 feedback from the citizen"
                + (comment != null && !comment.isBlank() ? ": \"" + comment + "\"" : ".");
        notifyDepartmentStaff(report, NotificationType.STATUS_CHANGED, title, message, rating);
    }

    @Transactional
    public void notifyNewAccount(User citizen) {
        String title = "New account awaiting approval";
        String message = citizen.getFullName() + " registered a new citizen account and is awaiting approval.";
        userRepository.findByRoleName(RoleName.ADMIN)
                .forEach(admin -> create(admin, null, NotificationType.NEW_ACCOUNT, title, message, null, citizen));
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
                        "Report " + report.getReportCode() + " (" + reportSummary(report) + ") has been "
                                + humanize(report.getStatus()) + " for longer than expected.");
            }
        }
    }

    @Transactional
    public void notifyDepartmentMention(Report report, Department mentionedDepartment, User author) {
        String title = "Mentioned in a report comment";
        String message = author.getFullName() + " mentioned your department on report "
                + report.getReportCode() + " (" + reportSummary(report) + ").";
        userRepository.findByDepartmentId(mentionedDepartment.getId())
                .forEach(staff -> create(staff, report, NotificationType.DEPARTMENT_MENTION, title, message));
    }

    private void notifyDepartmentStaff(Report report, NotificationType type, String title, String message) {
        notifyDepartmentStaff(report, type, title, message, null);
    }

    private void notifyDepartmentStaff(Report report, NotificationType type, String title, String message,
                                        Integer rating) {
        if (report.getDepartment() == null) {
            return;
        }
        userRepository.findByDepartmentId(report.getDepartment().getId())
                .forEach(staff -> create(staff, report, type, title, message, rating));
    }

    private void create(User recipient, Report report, NotificationType type, String title, String message) {
        create(recipient, report, type, title, message, null, null);
    }

    private void create(User recipient, Report report, NotificationType type, String title, String message,
                         Integer rating) {
        create(recipient, report, type, title, message, rating, null);
    }

    private void create(User recipient, Report report, NotificationType type, String title, String message,
                         Integer rating, User subjectUser) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setReport(report);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRating(rating);
        notification.setSubjectUser(subjectUser);
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
