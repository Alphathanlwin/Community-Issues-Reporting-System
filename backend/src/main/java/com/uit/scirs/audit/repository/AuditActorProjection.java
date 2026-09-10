package com.uit.scirs.audit.repository;

/** Distinct actor (id + display name) for the Audit Logs "actor" filter. */
public interface AuditActorProjection {

    Long getActorId();

    String getActorName();
}
