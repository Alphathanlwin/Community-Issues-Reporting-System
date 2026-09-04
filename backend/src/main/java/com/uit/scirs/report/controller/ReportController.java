package com.uit.scirs.report.controller;

import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.report.dto.AssignReportDTO;
import com.uit.scirs.report.dto.CreateReportCommentDTO;
import com.uit.scirs.report.dto.CreateReportDTO;
import com.uit.scirs.report.dto.RejectReportDTO;
import com.uit.scirs.report.dto.ReportCommentDTO;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.dto.ReportMapDTO;
import com.uit.scirs.report.dto.ReportStatusHistoryDTO;
import com.uit.scirs.report.dto.UpdateReportPriorityDTO;
import com.uit.scirs.report.dto.UpdateReportStatusDTO;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.service.ReportAssignmentService;
import com.uit.scirs.report.service.ReportCommentService;
import com.uit.scirs.report.service.ReportService;
import com.uit.scirs.report.service.ReportWorkflowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Validated
public class ReportController {

    private final ReportService reportService;
    private final ReportWorkflowService reportWorkflowService;
    private final ReportAssignmentService reportAssignmentService;
    private final ReportCommentService reportCommentService;

    public ReportController(ReportService reportService,
                             ReportWorkflowService reportWorkflowService,
                             ReportAssignmentService reportAssignmentService,
                             ReportCommentService reportCommentService) {
        this.reportService = reportService;
        this.reportWorkflowService = reportWorkflowService;
        this.reportAssignmentService = reportAssignmentService;
        this.reportCommentService = reportCommentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ReportDTO> createReport(@Valid @RequestPart("data") CreateReportDTO dto,
                                                   @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                                   @AuthenticationPrincipal CurrentUser currentUser) {
        ReportDTO created = reportService.createReport(dto, images, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<List<ReportDTO>> getReports(@RequestParam(required = false) ReportStatus status,
                                                       @RequestParam(required = false) Long categoryId,
                                                       @RequestParam(required = false) Long departmentId,
                                                       @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportService.getReports(currentUser, status, categoryId, departmentId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<ReportDTO>> getMyReports(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportService.getMyReports(currentUser.getId()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportDTO>> getPendingReports() {
        return ResponseEntity.ok(reportService.getPendingReports());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<ReportDTO> getReport(@PathVariable Long id,
                                                @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportService.getReportById(id, currentUser));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDTO> approve(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportWorkflowService.approve(id, currentUser));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDTO> reject(@PathVariable Long id, @Valid @RequestBody RejectReportDTO dto,
                                             @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportWorkflowService.reject(id, dto.getRejectionReason(), currentUser));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ReportDTO> changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateReportStatusDTO dto,
                                                   @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportWorkflowService.changeStatus(id, dto, currentUser));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ReportDTO> uploadCompletionPhotos(@PathVariable Long id,
                                                             @RequestPart("images") List<MultipartFile> images,
                                                             @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportService.uploadCompletionPhotos(id, images, currentUser));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<List<ReportStatusHistoryDTO>> getHistory(@PathVariable Long id,
                                                                     @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportService.getHistory(id, currentUser));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDTO> assign(@PathVariable Long id, @Valid @RequestBody AssignReportDTO dto,
                                             @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportAssignmentService.assign(id, dto, currentUser));
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ReportDTO> updatePriority(@PathVariable Long id, @Valid @RequestBody UpdateReportPriorityDTO dto,
                                                      @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportWorkflowService.updatePriority(id, dto, currentUser));
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<List<ReportCommentDTO>> getComments(@PathVariable Long id,
                                                                @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportCommentService.getComments(id, currentUser));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ReportCommentDTO> addComment(@PathVariable Long id, @Valid @RequestBody CreateReportCommentDTO dto,
                                                         @AuthenticationPrincipal CurrentUser currentUser) {
        ReportCommentDTO created = reportCommentService.addComment(id, dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/map")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<List<ReportMapDTO>> getMapPins(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal minLat,
            @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal maxLat,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal minLng,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal maxLng,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportService.getMapPins(categoryId, status, minLat, maxLat, minLng, maxLng, currentUser));
    }

    // Cached (see CacheConfig.PUBLIC_MAP) — unfiltered, citizen-visible pins only.
    @GetMapping("/map/public")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<List<ReportMapDTO>> getPublicMapPins() {
        return ResponseEntity.ok(reportService.getApprovedMapReports());
    }
}
