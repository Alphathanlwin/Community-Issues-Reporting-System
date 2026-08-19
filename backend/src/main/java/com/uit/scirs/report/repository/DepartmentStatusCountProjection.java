package com.uit.scirs.report.repository;

public interface DepartmentStatusCountProjection {
    Long getDepartmentId();
    Long getOpenCount();
    Long getResolvedCount();
}
