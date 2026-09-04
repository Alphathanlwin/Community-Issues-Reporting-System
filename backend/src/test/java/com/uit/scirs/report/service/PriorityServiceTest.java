package com.uit.scirs.report.service;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.report.entity.ReportPriority;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityServiceTest {

    private final PriorityService priorityService = new PriorityService();

    @Test
    void calculate_lowSeverityCategoryFreshReport_returnsLow() {
        Category park = category(2);

        PriorityService.PriorityResult result = priorityService.calculate(park, 0, LocalDateTime.now().minusHours(1));

        assertThat(result.score()).isEqualTo(2);
        assertThat(result.priority()).isEqualTo(ReportPriority.LOW);
    }

    @Test
    void calculate_highSeverityCategoryAgedReportWithDuplicates_returnsUrgent() {
        Category water = category(5);

        PriorityService.PriorityResult result =
                priorityService.calculate(water, 2, LocalDateTime.now().minusHours(100));

        assertThat(result.score()).isEqualTo(9);
        assertThat(result.priority()).isEqualTo(ReportPriority.URGENT);
    }

    @Test
    void calculate_severityThreeNoDuplicatesFreshReport_scoresExactlyThreeAndReturnsNormal() {
        Category sanitation = category(3);

        PriorityService.PriorityResult result =
                priorityService.calculate(sanitation, 0, LocalDateTime.now().minusHours(10));

        assertThat(result.score()).isEqualTo(3);
        assertThat(result.priority()).isEqualTo(ReportPriority.NORMAL);
    }

    @Test
    void calculate_moderateSeverityWithFewDuplicatesAndModerateAge_scoresSevenAndReturnsHigh() {
        Category roads = category(4);

        PriorityService.PriorityResult result =
                priorityService.calculate(roads, 3, LocalDateTime.now().minusHours(50));

        assertThat(result.score()).isEqualTo(7);
        assertThat(result.priority()).isEqualTo(ReportPriority.HIGH);
    }

    @Test
    void calculate_manyDuplicatesOnFreshMinorReport_scoresExactlyFiveAtHighBoundary() {
        Category park = category(1);

        PriorityService.PriorityResult result = priorityService.calculate(park, 5, LocalDateTime.now());

        assertThat(result.score()).isEqualTo(5);
        assertThat(result.priority()).isEqualTo(ReportPriority.HIGH);
    }

    @Test
    void calculate_manyDuplicatesOnModerateSeverityFreshReport_scoresExactlyEightAtUrgentBoundary() {
        Category buildings = category(4);

        PriorityService.PriorityResult result = priorityService.calculate(buildings, 5, LocalDateTime.now());

        assertThat(result.score()).isEqualTo(8);
        assertThat(result.priority()).isEqualTo(ReportPriority.URGENT);
    }

    private Category category(int severityWeight) {
        Category category = new Category();
        category.setSeverityWeight(severityWeight);
        return category;
    }
}
