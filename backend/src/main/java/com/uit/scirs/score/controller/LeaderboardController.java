package com.uit.scirs.score.controller;

import com.uit.scirs.score.dto.LeaderboardEntryDTO;
import com.uit.scirs.score.service.ScoreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@Tag(name = "Leaderboard", description = "Ranked citizens by score points")
public class LeaderboardController {

    private final ScoreService scoreService;

    public LeaderboardController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboard(
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return ResponseEntity.ok(scoreService.getLeaderboard(limit));
    }
}
