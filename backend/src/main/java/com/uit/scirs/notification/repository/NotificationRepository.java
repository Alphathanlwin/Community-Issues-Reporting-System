package com.uit.scirs.notification.repository;

import com.uit.scirs.notification.entity.Notification;
import com.uit.scirs.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    boolean existsByReportIdAndTypeAndCreatedAtAfter(Long reportId, NotificationType type, LocalDateTime after);
}
