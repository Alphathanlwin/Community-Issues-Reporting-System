package com.uit.scirs.feedback.controller;

import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.feedback.dto.CreateFeedbackDTO;
import com.uit.scirs.feedback.dto.FeedbackDTO;
import com.uit.scirs.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Feedback", description = "One rating+comment per resolved report, from its reporter")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<FeedbackDTO> create(@Valid @RequestBody CreateFeedbackDTO dto,
                                               @AuthenticationPrincipal CurrentUser currentUser) {
        FeedbackDTO created = feedbackService.create(dto, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<FeedbackDTO> getByReportId(@PathVariable Long reportId,
                                                      @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(feedbackService.getByReportId(reportId, currentUser));
    }
}
