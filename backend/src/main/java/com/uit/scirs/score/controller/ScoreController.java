package com.uit.scirs.score.controller;

import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.score.dto.ScoreSummaryDTO;
import com.uit.scirs.score.service.ScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/score")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ScoreSummaryDTO> getMyScore(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(scoreService.getMyScore(currentUser.getId()));
    }
}
