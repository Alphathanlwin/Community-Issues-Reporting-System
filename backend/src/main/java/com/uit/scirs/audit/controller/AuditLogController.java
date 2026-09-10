package com.uit.scirs.audit.controller;

import com.uit.scirs.audit.dto.AuditActorDTO;
import com.uit.scirs.audit.dto.AuditLogDTO;
import com.uit.scirs.audit.entity.AuditAction;
import com.uit.scirs.audit.service.AuditService;
import com.uit.scirs.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only feed of the immutable audit trail. Admin-only — the log records
 * who did what across every console, so it is never exposed to staff or
 * citizens. Rows are written by {@code AuditService.record} from the services
 * that mutate state; there is no write endpoint.
 */
@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Logs", description = "Immutable trail of administrative and staff operations (admin-only, read-only)")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    // Newest-first, paged. `action` / `actorId` narrow to one operation kind
    // or one operator; `search` matches actor, target, or details text;
    // `startDate` / `endDate` are yyyy-MM-dd and bound the timestamp inclusively.
    @GetMapping
    public ResponseEntity<PageResponse<AuditLogDTO>> list(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(auditService.search(action, actorId, search, startDate, endDate, pageable));
    }

    @GetMapping("/actors")
    public ResponseEntity<List<AuditActorDTO>> actors() {
        return ResponseEntity.ok(auditService.getActors());
    }
}
