package com.uit.scirs.report.service;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.integration.FileStorageService;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.common.util.ReportCodeGenerator;
import com.uit.scirs.notification.service.NotificationService;
import com.uit.scirs.report.dto.CreateReportDTO;
import com.uit.scirs.report.dto.PossibleDuplicateDTO;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.dto.ReportMapDTO;
import com.uit.scirs.report.dto.ReportSubmissionResultDTO;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportConfirmation;
import com.uit.scirs.report.entity.ReportImage;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.mapper.ReportMapper;
import com.uit.scirs.report.repository.ReportConfirmationRepository;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.report.repository.ReportStatusHistoryRepository;
import com.uit.scirs.score.entity.PointReason;
import com.uit.scirs.score.service.ScoreService;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock ReportRepository reportRepository;
    @Mock ReportStatusHistoryRepository reportStatusHistoryRepository;
    @Mock ReportConfirmationRepository reportConfirmationRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @Mock ReportMapper reportMapper;
    @Mock FileStorageService fileStorageService;
    @Mock ReportCodeGenerator reportCodeGenerator;
    @Mock NotificationService notificationService;
    @Mock DuplicateDetectionService duplicateDetectionService;
    @Mock ScoreService scoreService;
    @InjectMocks ReportService reportService;

    @Test
    void createReport_withApprovedCitizen_savesReportWithPendingApprovalStatusAndOwner() {
        User citizen = approvedCitizen(7L);
        Category category = category(3L, "Pothole / Damaged Road");
        CreateReportDTO dto = createDto(3L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(reportCodeGenerator.next()).thenReturn("RPT-2026-000001");
        when(reportMapper.toEntity(dto)).thenAnswer(i -> {
            Report report = new Report();
            report.setTitle(dto.getTitle());
            report.setDescription(dto.getDescription());
            report.setLatitude(dto.getLatitude());
            report.setLongitude(dto.getLongitude());
            return report;
        });
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> {
            Report saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(reportMapper.toDTO(any(Report.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        ReportDTO result = reportService.createReport(dto, null, 7L);

        assertThat(result.getStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(result.getReporterId()).isEqualTo(7L);
        assertThat(result.getReportCode()).isEqualTo("RPT-2026-000001");

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getReporter()).isEqualTo(citizen);
        assertThat(captor.getValue().getCategory()).isEqualTo(category);
        assertThat(captor.getValue().getStatus()).isEqualTo(ReportStatus.PENDING_APPROVAL);
        verify(notificationService).notifyNewReport(captor.getValue());
    }

    @Test
    void createReport_withUnapprovedCitizen_throwsBusinessRuleException() {
        User citizen = approvedCitizen(7L);
        citizen.setAccountStatus(AccountStatus.PENDING);
        CreateReportDTO dto = createDto(3L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));

        assertThatThrownBy(() -> reportService.createReport(dto, null, 7L))
                .isInstanceOf(BusinessRuleException.class);

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void createReport_withUnknownCategory_throwsResourceNotFoundException() {
        User citizen = approvedCitizen(7L);
        CreateReportDTO dto = createDto(99L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createReport(dto, null, 7L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void createReport_withUnknownCitizen_throwsResourceNotFoundException() {
        CreateReportDTO dto = createDto(3L);
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createReport(dto, null, 7L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void createReport_withImage_invokesFileStorageAndSavesReturnedUrl() {
        User citizen = approvedCitizen(7L);
        Category category = category(3L, "Pothole / Damaged Road");
        CreateReportDTO dto = createDto(3L);
        MultipartFile image = new MockMultipartFile("images", "pothole.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(reportCodeGenerator.next()).thenReturn("RPT-2026-000002");
        when(reportMapper.toEntity(dto)).thenReturn(new Report());
        when(fileStorageService.store(eq(image), anyString())).thenReturn("/uploads/reports/generated.jpg");
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> {
            Report saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(reportMapper.toDTO(any(Report.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        reportService.createReport(dto, List.of(image), 7L);

        verify(fileStorageService).store(eq(image), anyString());

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        List<ReportImage> savedImages = captor.getValue().getImages();
        assertThat(savedImages).hasSize(1);
        assertThat(savedImages.get(0).getImageUrl()).isEqualTo("/uploads/reports/generated.jpg");
        assertThat(savedImages.get(0).getUploadedBy()).isEqualTo(citizen);
    }

    @Test
    void submitReport_noDuplicatesFound_createsReportNormally() {
        User citizen = approvedCitizen(7L);
        Category category = category(3L, "Pothole / Damaged Road");
        CreateReportDTO dto = createDto(3L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(duplicateDetectionService.findPossibleDuplicates(dto.getLatitude(), dto.getLongitude(), 3L))
                .thenReturn(List.of());
        when(reportCodeGenerator.next()).thenReturn("RPT-2026-000010");
        when(reportMapper.toEntity(dto)).thenReturn(new Report());
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> {
            Report saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(reportMapper.toDTO(any(Report.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        ReportSubmissionResultDTO result = reportService.submitReport(dto, null, 7L);

        assertThat(result.getOutcome()).isEqualTo(ReportSubmissionResultDTO.Outcome.CREATED);
        assertThat(result.getReport().getStatus()).isEqualTo("PENDING_APPROVAL");

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().isDuplicateChecked()).isFalse();
    }

    @Test
    void submitReport_duplicatesFound_returnsThemWithoutCreatingAReport() {
        User citizen = approvedCitizen(7L);
        CreateReportDTO dto = createDto(3L);
        PossibleDuplicateDTO duplicate = new PossibleDuplicateDTO();
        duplicate.setReportId(42L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(duplicateDetectionService.findPossibleDuplicates(dto.getLatitude(), dto.getLongitude(), 3L))
                .thenReturn(List.of(duplicate));

        ReportSubmissionResultDTO result = reportService.submitReport(dto, null, 7L);

        assertThat(result.getOutcome()).isEqualTo(ReportSubmissionResultDTO.Outcome.DUPLICATES_FOUND);
        assertThat(result.getDuplicateCheck().getPossibleDuplicates()).containsExactly(duplicate);
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void submitReport_withForceCreate_skipsDuplicateCheckAndMarksDuplicateChecked() {
        User citizen = approvedCitizen(7L);
        Category category = category(3L, "Pothole / Damaged Road");
        CreateReportDTO dto = createDto(3L);
        dto.setForceCreate(true);

        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(reportCodeGenerator.next()).thenReturn("RPT-2026-000011");
        when(reportMapper.toEntity(dto)).thenReturn(new Report());
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> {
            Report saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(reportMapper.toDTO(any(Report.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        ReportSubmissionResultDTO result = reportService.submitReport(dto, null, 7L);

        assertThat(result.getOutcome()).isEqualTo(ReportSubmissionResultDTO.Outcome.CREATED);
        verify(duplicateDetectionService, never()).findPossibleDuplicates(any(), any(), any());

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().isDuplicateChecked()).isTrue();
    }

    @Test
    void submitReport_withConfirmDuplicateOfId_createsConfirmationAndAwardsPointsInsteadOfANewReport() {
        User citizen = approvedCitizen(7L);
        Report existing = new Report();
        existing.setId(42L);
        CreateReportDTO dto = createDto(3L);
        dto.setConfirmDuplicateOfId(42L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(reportRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(reportConfirmationRepository.existsByReportIdAndCitizenId(42L, 7L)).thenReturn(false);
        when(reportMapper.toDTO(existing)).thenReturn(dtoFor(existing));

        ReportSubmissionResultDTO result = reportService.submitReport(dto, null, 7L);

        assertThat(result.getOutcome()).isEqualTo(ReportSubmissionResultDTO.Outcome.CONFIRMED);
        verify(reportRepository, never()).save(any(Report.class));

        ArgumentCaptor<ReportConfirmation> captor = ArgumentCaptor.forClass(ReportConfirmation.class);
        verify(reportConfirmationRepository).save(captor.capture());
        assertThat(captor.getValue().getReport()).isEqualTo(existing);
        assertThat(captor.getValue().getCitizen()).isEqualTo(citizen);
        verify(scoreService).award(citizen, PointReason.CONFIRMATION_GIVEN, existing);
    }

    @Test
    void submitReport_confirmingTheSameReportTwice_throwsDuplicateResourceException() {
        User citizen = approvedCitizen(7L);
        Report existing = new Report();
        existing.setId(42L);
        CreateReportDTO dto = createDto(3L);
        dto.setConfirmDuplicateOfId(42L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(reportRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(reportConfirmationRepository.existsByReportIdAndCitizenId(42L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> reportService.submitReport(dto, null, 7L))
                .isInstanceOf(DuplicateResourceException.class);

        verify(reportConfirmationRepository, never()).save(any(ReportConfirmation.class));
        verify(scoreService, never()).award(any(), any(), any());
    }

    @Test
    void getReportById_whenCitizenRequestsAnotherCitizensReport_throwsAccessDeniedException() {
        User owner = approvedCitizen(7L);
        Report report = new Report();
        report.setId(1L);
        report.setReporter(owner);
        report.setStatus(ReportStatus.PENDING_APPROVAL);

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        CurrentUser otherCitizen = new CurrentUser(99L, "other@example.com", RoleName.CITIZEN, null);

        assertThatThrownBy(() -> reportService.getReportById(1L, otherCitizen))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getReportById_whenStaffFromAnotherDepartment_throwsAccessDeniedException() {
        Report report = reportWithDepartment(1L, 2L);

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        CurrentUser staffFromOtherDept = new CurrentUser(50L, "staff@example.com", RoleName.STAFF, 3L);

        assertThatThrownBy(() -> reportService.getReportById(1L, staffFromOtherDept))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getMapPins_asCitizen_restrictsToPublicStatuses() {
        CurrentUser citizen = new CurrentUser(7L, "citizen@example.com", RoleName.CITIZEN, null);
        when(reportRepository.findForMap(null, null, null, null, null, null, true,
                List.of(ReportStatus.PENDING_APPROVAL, ReportStatus.REJECTED)))
                .thenReturn(List.of());
        when(reportMapper.toMapDTOList(List.of())).thenReturn(List.of());

        List<ReportMapDTO> result = reportService.getMapPins(null, null, null, null, null, null, citizen);

        assertThat(result).isEmpty();
        verify(reportRepository).findForMap(null, null, null, null, null, null, true,
                List.of(ReportStatus.PENDING_APPROVAL, ReportStatus.REJECTED));
    }

    @Test
    void getMapPins_asAdmin_doesNotRestrictStatuses() {
        CurrentUser admin = new CurrentUser(1L, "admin@example.com", RoleName.ADMIN, null);
        when(reportRepository.findForMap(3L, ReportStatus.ASSIGNED, null, null, null, null, false,
                List.of(ReportStatus.PENDING_APPROVAL, ReportStatus.REJECTED)))
                .thenReturn(List.of());
        when(reportMapper.toMapDTOList(List.of())).thenReturn(List.of());

        reportService.getMapPins(3L, ReportStatus.ASSIGNED, null, null, null, null, admin);

        verify(reportRepository).findForMap(3L, ReportStatus.ASSIGNED, null, null, null, null, false,
                List.of(ReportStatus.PENDING_APPROVAL, ReportStatus.REJECTED));
    }

    @Test
    void uploadCompletionPhotos_withNoImages_throwsBusinessRuleException() {
        Report report = reportWithDepartment(1L, 2L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        CurrentUser staff = new CurrentUser(50L, "staff@example.com", RoleName.STAFF, 2L);

        assertThatThrownBy(() -> reportService.uploadCompletionPhotos(1L, List.of(), staff))
                .isInstanceOf(BusinessRuleException.class);

        verify(reportRepository, never()).save(any(Report.class));
    }

    private Report reportWithDepartment(Long reportId, Long departmentId) {
        com.uit.scirs.department.entity.Department department = new com.uit.scirs.department.entity.Department();
        department.setId(departmentId);
        Report report = new Report();
        report.setId(reportId);
        report.setDepartment(department);
        report.setStatus(ReportStatus.ASSIGNED);
        report.setReporter(approvedCitizen(7L));
        return report;
    }

    private User approvedCitizen(Long id) {
        User user = new User();
        user.setId(id);
        user.setFullName("Test Citizen");
        user.setEmail("citizen@example.com");
        user.setAccountStatus(AccountStatus.APPROVED);
        return user;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setActive(true);
        return category;
    }

    private CreateReportDTO createDto(Long categoryId) {
        CreateReportDTO dto = new CreateReportDTO();
        dto.setTitle("Pothole on Main St");
        dto.setDescription("Large pothole blocking traffic");
        dto.setCategoryId(categoryId);
        dto.setLatitude(new BigDecimal("16.8409000"));
        dto.setLongitude(new BigDecimal("96.1735000"));
        return dto;
    }

    private ReportDTO dtoFor(Report entity) {
        ReportDTO dto = new ReportDTO();
        dto.setId(entity.getId());
        dto.setReportCode(entity.getReportCode());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : ReportStatus.PENDING_APPROVAL.name());
        if (entity.getReporter() != null) {
            dto.setReporterId(entity.getReporter().getId());
        }
        return dto;
    }
}
