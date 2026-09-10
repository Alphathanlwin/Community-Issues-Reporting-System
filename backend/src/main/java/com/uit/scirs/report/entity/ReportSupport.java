package com.uit.scirs.report.entity;

import com.uit.scirs.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A citizen backing ("+1 / support") someone else's report from the
 * "What's happening in Yangon" feed. One row per (report, citizen) —
 * enforced by the unique constraint below and pre-checked in
 * {@code ReportService} before insert, mirroring {@link ReportConfirmation}.
 * Each support awards the supporter {@code PointReason.SUPPORT_GIVEN} points,
 * capped at five per rolling 24 hours (enforced in the service).
 */
@Entity
@Table(name = "report_supports",
        uniqueConstraints = @UniqueConstraint(columnNames = {"report_id", "citizen_id"}))
public class ReportSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ReportSupport() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public User getCitizen() {
        return citizen;
    }

    public void setCitizen(User citizen) {
        this.citizen = citizen;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
