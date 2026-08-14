package com.uit.scirs.report.service;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.integration.FileStorageService;
import com.uit.scirs.common.util.ReportCodeGenerator;
import com.uit.scirs.report.dto.CreateReportDTO;
import com.uit.scirs.report.dto.ReportDTO;
import com.uit.scirs.report.entity.ImageType;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportImage;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.mapper.ReportMapper;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ReportService {

    private static final String IMAGE_FOLDER = "reports";

    private final ReportRepository reportRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;
    private final FileStorageService fileStorageService;
    private final ReportCodeGenerator reportCodeGenerator;

    public ReportService(ReportRepository reportRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository,
                          ReportMapper reportMapper,
                          FileStorageService fileStorageService,
                          ReportCodeGenerator reportCodeGenerator) {
        this.reportRepository = reportRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.reportMapper = reportMapper;
        this.fileStorageService = fileStorageService;
        this.reportCodeGenerator = reportCodeGenerator;
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
        return reportMapper.toDTO(saved);
    }
}
