package com.uit.scirs.score.dto;

import java.util.List;

public class ScoreSummaryDTO {

    private Integer totalPoints;
    private List<PointTransactionDTO> history;

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public List<PointTransactionDTO> getHistory() {
        return history;
    }

    public void setHistory(List<PointTransactionDTO> history) {
        this.history = history;
    }
}
