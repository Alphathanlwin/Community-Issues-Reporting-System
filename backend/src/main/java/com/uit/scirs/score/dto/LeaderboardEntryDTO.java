package com.uit.scirs.score.dto;

public class LeaderboardEntryDTO {

    private int rank;
    private Long userId;
    private String fullName;
    private Integer scorePoints;

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getScorePoints() {
        return scorePoints;
    }

    public void setScorePoints(Integer scorePoints) {
        this.scorePoints = scorePoints;
    }
}
