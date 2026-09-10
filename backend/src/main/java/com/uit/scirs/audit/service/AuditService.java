package com.uit.scirs.audit.service;

import com.uit.scirs.audit.dto.AuditActorDTO;
import com.uit.scirs.audit.dto.AuditLogDTO;
import com.uit.scirs.audit.entity.AuditAction;
import com.uit.scirs.audit.entity.AuditLog;
import com.uit.scirs.audit.mapper.AuditLogMapper;
import com.uit.scirs.audit.repository.AuditLogRepository;
import com.uit.scirs.common.dto.PageResponse;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Writes and reads the immutable {@code audit_logs} trail.
 *
 * <p>{@link #record} is called from the service methods that mutate state
 * (account reviews, department/category changes, report transitions). It is
 * best-effort: the acting user is read from the security context, the write
 * happens in a separate transaction, and any failure is logged and
 * swallowed so auditing can never break the operation it is recording.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private static final LocalDateTime MIN_DATE = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX_DATE = LocalDateTime.of(9999, 12, 31, 0, 0);

    private final AuditLogRepository auditLogRepository;
    private final AuditLogWriter auditLogWriter;
    private final AuditLogMapper auditLogMapper;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository,
                        AuditLogWriter auditLogWriter,
                        AuditLogMapper auditLogMapper,
                        UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogWriter = auditLogWriter;
        this.auditLogMapper = auditLogMapper;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> search(AuditAction action,
                                            Long actorId,
                                            String search,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            Pageable pageable) {
        String pattern = (search == null || search.isBlank())
                ? "%"
                : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        LocalDateTime from = startDate != null ? startDate.atStartOfDay() : MIN_DATE;
        LocalDateTime to = endDate != null ? endDate.plusDays(1).atStartOfDay() : MAX_DATE;

        return PageResponse.from(
                auditLogRepository.search(action, actorId, pattern, from, to, pageable)
                        .map(auditLogMapper::toDTO));
    }

    @Transactional(readOnly = true)
    public List<AuditActorDTO> getActors() {
        return auditLogRepository.findDistinctActors().stream()
                .map(p -> new AuditActorDTO(p.getActorId(), p.getActorName()))
                .toList();
    }

    /**
     * Append an audit row for {@code action} on the given target. The acting
     * user is taken from the security context. Never throws.
     */
    public void record(AuditAction action, String targetType, Long targetId, String targetLabel, String details) {
        try {
            AuditLog entry = new AuditLog();
            CurrentUser actor = currentActor();
            if (actor != null) {
                entry.setActorId(actor.getId());
                entry.setActorEmail(actor.getEmail());
                entry.setActorName(userRepository.findById(actor.getId())
                        .map(u -> u.getFullName())
                        .orElse(actor.getEmail() != null ? actor.getEmail() : "Unknown"));
            } else {
                entry.setActorName("System");
            }
            entry.setAction(action);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId);
            entry.setTargetLabel(clamp(targetLabel != null ? targetLabel : "—", 300));
            entry.setDetails(details != null ? clamp(details, 500) : null);

            auditLogWriter.write(entry);
        } catch (Exception ex) {
            log.warn("Audit write failed for {} on {}#{}", action, targetType, targetId, ex);
        }
    }

    private CurrentUser currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser currentUser) {
            return currentUser;
        }
        return null;
    }

    private static String clamp(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
