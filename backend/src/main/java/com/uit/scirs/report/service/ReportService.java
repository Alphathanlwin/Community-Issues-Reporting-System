package com.uit.scirs.report.service;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.common.config.CacheConfig;
import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.integration.FileStorageService;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.common.util.ReportCodeGenerator;
import com.uit.scirs.notification.service.NotificationService;
import com.uit.scirs.report.dto.CreateReportDTO;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.dto.ReportMapDTO;
import com.uit.scirs.report.dto.ReportStatusHistoryDTO;
import com.uit.scirs.report.entity.ImageType;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportImage;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.mapper.ReportMapper;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.report.repository.ReportStatusHistoryRepository;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private static final String IMAGE_FOLDER = "reports";
    private static final List<ReportStatus> HIDDEN_FROM_CITIZENS =
            List.of(ReportStatus.PENDING_APPROVAL, ReportStatus.REJECTED);

    private final ReportRepository reportRepository;
    private final ReportStatusHistoryRepository reportStatusHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;
    private final FileStorageService fileStorageService;
    private final ReportCodeGenerator reportCodeGenerator;
    private final NotificationService notificationService;
    private final Long slaWaitingTooLongHours;

    public ReportService(ReportRepository reportRepository,
                          ReportStatusHistoryRepository reportStatusHistoryRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository,
                          ReportMapper reportMapper,
                          FileStorageService fileStorageService,
                          ReportCodeGenerator reportCodeGenerator,
                          NotificationService notificationService,
                          @Value("${app.sla.waiting-too-long-hours:48}") Long slaWaitingTooLongHours) {
        this.reportRepository = reportRepository;
        this.reportStatusHistoryRepository = reportStatusHistoryRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.reportMapper = reportMapper;
        this.fileStorageService = fileStorageService;
        this.reportCodeGenerator = reportCodeGenerator;
        this.notificationService = notificationService;
        this.slaWaitingTooLongHours = slaWaitingTooLongHours;
    }

    @Transactional
    public ReportDTO createReport(CreateReportDTO dto, List<MultipartFile> images, Long citizenId) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (citizen.getAccountStatus() != AccountStatus.APPROVED) {
            throw new BusinessRuleException("Your account is not yet approved to submit reports.");
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));

        Report report = reportMapper.toEntity(dto);
        report.setReporter(citizen);
        report.setCategory(category);
        report.setStatus(ReportStatus.PENDING_APPROVAL);
        report.setReportCode(reportCodeGenerator.next());

        if (images != null) {
            for (MultipartFile image : images) {
                if (image == null || image.isEmpty()) {
                    continue;
                }
                String imageUrl = fileStorageService.store(image, IMAGE_FOLDER);

                ReportImage reportImage = new ReportImage();
                reportImage.setReport(report);
                reportImage.setImageUrl(imageUrl);
                reportImage.setImageType(ImageType.REPORT_PHOTO);
                reportImage.setUploadedBy(citizen);
                report.getImages().add(reportImage);
            }
        }

        Report saved = reportRepository.save(report);
        notificationService.notifyNewReport(saved);
        return reportMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public ReportDTO getReportById(Long id, CurrentUser currentUser) {
        Report report = findEntity(id);
        assertCanView(report, currentUser);
        return reportMapper.toDTO(report);
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> getMyReports(Long citizenId) {
        return reportMapper.toDTOList(reportRepository.findByReporterIdOrderByCreatedAtDesc(citizenId));
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> getPendingReports() {
        return reportMapper.toDTOList(reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING_APPROVAL));
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> getReports(CurrentUser currentUser, ReportStatus status, Long categoryId, Long departmentId) {
        Long effectiveDepartmentId = departmentId;
        if (currentUser.getRole() == RoleName.STAFF) {
            effectiveDepartmentId = currentUser.getDepartmentId();
        }
        return reportMapper.toDTOList(reportRepository.search(status, categoryId, effectiveDepartmentId));
    }

    @Transactional(readOnly = true)
    public List<ReportMapDTO> getMapPins(Long categoryId, ReportStatus status,
                                          BigDecimal minLat, BigDecimal maxLat,
                                          BigDecimal minLng, BigDecimal maxLng,
                                          CurrentUser currentUser) {
        boolean restrictToPublic = currentUser.getRole() == RoleName.CITIZEN;
        List<Report> reports = reportRepository.findForMap(categoryId, status, minLat, maxLat, minLng, maxLng,
                restrictToPublic, HIDDEN_FROM_CITIZENS);
        return reportMapper.toMapDTOList(reports);
    }

    /**
     * Unfiltered, citizen-visible map pins — the same subset any citizen may
     * see, so it's safe to share one cache entry across every caller
     * regardless of role. Deliberately NOT the same method as
     * {@link #getMapPins}: that one takes per-request filters and honors
     * role-based visibility (staff/admins additionally see PENDING_APPROVAL
     * and REJECTED reports), so caching it directly by its argument list
     * risks serving one user's role-restricted result to another.
     */
    @Cacheable(CacheConfig.PUBLIC_MAP)
    @Transactional(readOnly = true)
    public List<ReportMapDTO> getApprovedMapReports() {
        List<Report> reports = reportRepository.findForMap(null, null, null, null, null, null,
                true, HIDDEN_FROM_CITIZENS);
        return reportMapper.toMapDTOList(reports);
    }

    @Transactional
    public ReportDTO uploadCompletionPhotos(Long reportId, List<MultipartFile> images, CurrentUser currentUser) {
        Report report = findEntity(reportId);
        assertCanView(report, currentUser);

        if (images == null || images.stream().allMatch(image -> image == null || image.isEmpty())) {
            throw new BusinessRuleException("At least one completion photo is required.");
        }

        User uploader = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }
            String imageUrl = fileStorageService.store(image, IMAGE_FOLDER);

            ReportImage reportImage = new ReportImage();
            reportImage.setReport(report);
            reportImage.setImageUrl(imageUrl);
            reportImage.setImageType(ImageType.RESOLUTION_PHOTO);
            reportImage.setUploadedBy(uploader);
            report.getImages().add(reportImage);
        }

        return reportMapper.toDTO(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<ReportStatusHistoryDTO> getHistory(Long reportId, CurrentUser currentUser) {
        Report report = findEntity(reportId);
        assertCanView(report, currentUser);
        return reportMapper.toHistoryDTOList(reportStatusHistoryRepository.findByReportIdOrderByChangedAtAsc(reportId));
    }

    /** Hourly sweep for reports stuck in ASSIGNED/IN_PROGRESS past the SLA window. */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void sweepWaitingTooLong() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(slaWaitingTooLongHours);
        List<Report> stale = reportRepository.findByStatusInAndUpdatedAtBefore(
                List.of(ReportStatus.ASSIGNED, ReportStatus.IN_PROGRESS), cutoff);
        notificationService.sweepWaitingTooLong(stale);
    }

    private void assertCanView(Report report, CurrentUser user) {
        switch (user.getRole()) {
            case ADMIN -> {
                // full access
            }
            case STAFF -> {
                Long departmentId = report.getDepartment() != null ? report.getDepartment().getId() : null;
                if (departmentId == null || !departmentId.equals(user.getDepartmentId())) {
                    throw new AccessDeniedException("This report belongs to another department.");
                }
            }
            case CITIZEN -> {
                if (!report.getReporter().getId().equals(user.getId())) {
                    throw new AccessDeniedException("You can only view your own reports.");
                }
            }
        }
    }

    private Report findEntity(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
    }
}
