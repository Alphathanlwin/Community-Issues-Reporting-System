package com.uit.scirs.report.dto;

import java.util.List;

/**
 * Returned instead of a {@link ReportDTO} (HTTP 200, not 201) when
 * {@code POST /api/reports} finds open reports of the same category within
 * 100m. The citizen picks one to confirm ({@code confirmDuplicateOfId}) or
 * resubmits with {@code forceCreate: true}.
 */
public class DuplicateCheckResultDTO {

    private List<PossibleDuplicateDTO> possibleDuplicates;

    public List<PossibleDuplicateDTO> getPossibleDuplicates() {
        return possibleDuplicates;
    }

    public void setPossibleDuplicates(List<PossibleDuplicateDTO> possibleDuplicates) {
        this.possibleDuplicates = possibleDuplicates;
    }
}
