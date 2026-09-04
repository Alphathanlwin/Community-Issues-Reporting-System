package com.uit.scirs.report.dto;

/**
 * The three shapes {@code POST /api/reports} can now return, so
 * {@code ReportService.submitReport()} can hand the controller one result
 * and let it pick the HTTP status (201 created, 200 confirmed/duplicates
 * found) without any business logic living in the controller itself.
 */
public class ReportSubmissionResultDTO {

    public enum Outcome { CREATED, CONFIRMED, DUPLICATES_FOUND }

    private final Outcome outcome;
    private final ReportDTO report;
    private final DuplicateCheckResultDTO duplicateCheck;

    private ReportSubmissionResultDTO(Outcome outcome, ReportDTO report, DuplicateCheckResultDTO duplicateCheck) {
        this.outcome = outcome;
        this.report = report;
        this.duplicateCheck = duplicateCheck;
    }

    public static ReportSubmissionResultDTO created(ReportDTO report) {
        return new ReportSubmissionResultDTO(Outcome.CREATED, report, null);
    }

    public static ReportSubmissionResultDTO confirmed(ReportDTO report) {
        return new ReportSubmissionResultDTO(Outcome.CONFIRMED, report, null);
    }

    public static ReportSubmissionResultDTO duplicatesFound(DuplicateCheckResultDTO duplicateCheck) {
        return new ReportSubmissionResultDTO(Outcome.DUPLICATES_FOUND, null, duplicateCheck);
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public ReportDTO getReport() {
        return report;
    }

    public DuplicateCheckResultDTO getDuplicateCheck() {
        return duplicateCheck;
    }
}
