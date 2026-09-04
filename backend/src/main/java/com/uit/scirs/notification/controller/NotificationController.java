package com.uit.scirs.notification.controller;

import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.notification.dto.NotificationDTO;
import com.uit.scirs.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Own notifications: list, unread count, mark read")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<List<NotificationDTO>> list(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(notificationService.list(currentUser.getId(), unreadOnly));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(currentUser.getId())));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<NotificationDTO> markRead(@PathVariable Long id,
                                                      @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(notificationService.markRead(id, currentUser.getId()));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal CurrentUser currentUser) {
        notificationService.markAllRead(currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
