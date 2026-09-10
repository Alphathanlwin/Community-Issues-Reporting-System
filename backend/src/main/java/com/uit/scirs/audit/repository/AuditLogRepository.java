package com.uit.scirs.audit.repository;

import com.uit.scirs.audit.entity.AuditAction;
import com.uit.scirs.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Newest-first, paged audit feed for the admin console. {@code action} and
     * {@code actorId} are optional (a null skips that filter). {@code
     * searchPattern}, {@code from} and {@code to} are ALWAYS bound to a
     * non-null value by the service ("%" and open-ended sentinel dates when
     * the caller supplies nothing) — a bare "IS NULL" test over a null String
     * or LocalDateTime bind makes Hibernate 6 emit an untyped parameter that
     * PostgreSQL rejects (see the same note on ReportRepository.searchReports).
     * {@code searchPattern} is pre-lowercased and already wrapped in '%'.
     */
    @Query("""
           SELECT a FROM AuditLog a
           WHERE (:action IS NULL OR a.action = :action)
             AND (:actorId IS NULL OR a.actorId = :actorId)
             AND (LOWER(a.actorName) LIKE :searchPattern
                  OR LOWER(COALESCE(a.actorEmail, '')) LIKE :searchPattern
                  OR LOWER(a.targetLabel) LIKE :searchPattern
                  OR LOWER(COALESCE(a.details, '')) LIKE :searchPattern)
             AND a.createdAt >= :from
             AND a.createdAt < :to
           """)
    Page<AuditLog> search(AuditAction action,
                          Long actorId,
                          String searchPattern,
                          LocalDateTime from,
                          LocalDateTime to,
                          Pageable pageable);

    /** Distinct actors that have ever appeared in the log — powers the "actor" filter dropdown. */
    @Query("""
           SELECT DISTINCT a.actorId AS actorId, a.actorName AS actorName
           FROM AuditLog a
           WHERE a.actorId IS NOT NULL
           ORDER BY a.actorName
           """)
    List<AuditActorProjection> findDistinctActors();
}
