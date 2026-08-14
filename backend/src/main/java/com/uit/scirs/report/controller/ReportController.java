package com.uit.scirs.report.controller;

import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.report.dto.CreateReportDTO;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ReportDTO> createReport(@Valid @RequestPart("data") CreateReportDTO dto,
                                                   @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                                   @AuthenticationPrincipal CurrentUser currentUser) {
        ReportDTO created = reportService.createReport(dto, images, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
