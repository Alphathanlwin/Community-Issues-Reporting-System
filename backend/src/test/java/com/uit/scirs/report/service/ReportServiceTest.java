package com.uit.scirs.report.service;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.integration.FileStorageService;
import com.uit.scirs.common.util.ReportCodeGenerator;
import com.uit.scirs.report.dto.CreateReportDTO;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportImage;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.mapper.ReportMapper;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
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
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @Mock ReportMapper reportMapper;
    @Mock FileStorageService fileStorageService;
    @Mock ReportCodeGenerator reportCodeGenerator;
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
