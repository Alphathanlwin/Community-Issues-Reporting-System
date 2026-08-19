package com.uit.scirs.report.repository;

import java.time.LocalDateTime;

public interface ResolutionTimeProjection {
    Long getDepartmentId();
    LocalDateTime getApprovedAt();
    LocalDateTime getResolvedAt();
}
