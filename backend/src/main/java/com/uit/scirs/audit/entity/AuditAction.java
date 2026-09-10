package com.uit.scirs.audit.entity;

/**
 * The set of administrative / staff operations recorded in {@code audit_logs}.
 * One value per meaningful state change an operator can make; read-only
 * actions are never audited. Persisted as a string (see {@link AuditLog}).
 */
public enum AuditAction {

    USER_APPROVED,
    USER_REJECTED,
    USER_SUSPENDED,
    STAFF_CREATED,

    DEPARTMENT_CREATED,
    DEPARTMENT_UPDATED,
    DEPARTMENT_DEACTIVATED,

    CATEGORY_CREATED,
    CATEGORY_UPDATED,
    CATEGORY_DEACTIVATED,

    REPORT_APPROVED,
    REPORT_REJECTED,
    REPORT_STATUS_CHANGED,
    REPORT_ASSIGNED,
    REPORT_PRIORITY_CHANGED
}
