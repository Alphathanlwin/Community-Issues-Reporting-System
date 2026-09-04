package com.uit.scirs.report.service;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.report.entity.ReportPriority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Calculates a report's priority automatically at approval time. Never
 * invoked from a controller and never sets {@code report.priority} itself —
 * {@link ReportWorkflowService#approve} owns the write, matching CLAUDE.md's
 * rule that only {@code ReportWorkflowService} mutates report state.
 *
 * <p>Score = category severity weight (1–5) + duplicate-count bonus (0/2/4,
 * always 0 for now — {@code duplicateCount} is accepted ahead of future
 * duplicate-detection work) + report-age bonus (0/1/2). Thresholds: score
 * &gt;= 8 -&gt; URGENT, &gt;= 5 -&gt; HIGH, &gt;= 3 -&gt; NORMAL, else LOW.
 */
@Service
public class PriorityService {

    public PriorityResult calculate(Category category, int duplicateCount, LocalDateTime reportCreatedAt) {
        int score = category.getSeverityWeight()
                + duplicateScore(duplicateCount)
                + ageScore(reportCreatedAt);

        ReportPriority priority;
        if (score >= 8) {
            priority = ReportPriority.URGENT;
        } else if (score >= 5) {
            priority = ReportPriority.HIGH;
        } else if (score >= 3) {
            priority = ReportPriority.NORMAL;
        } else {
            priority = ReportPriority.LOW;
        }

        return new PriorityResult(priority, score);
    }

    private int duplicateScore(int duplicateCount) {
        if (duplicateCount >= 5) {
            return 4;
        }
        if (duplicateCount >= 2) {
            return 2;
        }
        return 0;
    }

    private int ageScore(LocalDateTime reportCreatedAt) {
        long hours = Duration.between(reportCreatedAt, LocalDateTime.now()).toHours();
        if (hours > 72) {
            return 2;
        }
        if (hours > 24) {
            return 1;
        }
        return 0;
    }

    public record PriorityResult(ReportPriority priority, int score) {
    }
}
