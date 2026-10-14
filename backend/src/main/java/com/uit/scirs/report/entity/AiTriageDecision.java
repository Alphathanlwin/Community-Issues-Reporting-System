package com.uit.scirs.report.entity;

/**
 * Triage decision reached by the AI Vision & Auto-Triage Engine or subsequently
 * modified by human oversight.
 */
public enum AiTriageDecision {
    /** High confidence, clear photo, verified issue: auto-approved and routed directly to department. */
    AUTO_ROUTED,

    /** Low confidence, blurry image, description mismatch, or urgent safety hazard: queued for Admin review. */
    FLAGGED_FOR_REVIEW,

    /** Human administrator manually changed or rerouted the AI's triage decision. */
    MANUALLY_OVERRIDDEN
}
