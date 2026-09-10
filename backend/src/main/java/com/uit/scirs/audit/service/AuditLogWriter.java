package com.uit.scirs.audit.service;

import com.uit.scirs.audit.entity.AuditLog;
import com.uit.scirs.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists an audit row in its OWN transaction, decoupled from the operation
 * being audited. {@code REQUIRES_NEW} is deliberate: the audited action has
 * already succeeded and committed its own work by the time this runs, so an
 * audit-write failure must not mark the caller's transaction rollback-only
 * (the classic "caught the exception but the tx is already doomed" trap).
 * {@link AuditService#record} wraps the call and swallows any failure.
 */
@Component
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;

    public AuditLogWriter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditLog entry) {
        auditLogRepository.save(entry);
    }
}
