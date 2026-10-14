package com.uit.scirs.report.service;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.report.dto.AiTriageResultDTO;
import com.uit.scirs.report.entity.AiImageQuality;
import com.uit.scirs.report.entity.AiTriageDecision;
import com.uit.scirs.report.entity.Report;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiTriageServiceTest {

    private AiTriageService aiTriageService;
    private Category roadCategory;
    private Department roadDepartment;

    @BeforeEach
    void setUp() {
        aiTriageService = new AiTriageService();

        roadDepartment = new Department();
        roadDepartment.setId(10L);
        roadDepartment.setName("Roads & Bridges Department");

        roadCategory = new Category();
        roadCategory.setId(5L);
        roadCategory.setName("Pothole / Damaged Road Surface");
        roadCategory.setDepartment(roadDepartment);
    }

    @Test
    void evaluate_withNoImages_flagsForReviewWithNoImageTag() {
        Report report = new Report();
        report.setReportCode("RPT-AI-001");
        report.setTitle("Pothole on Main Street");
        report.setDescription("A deep pothole in the left lane");
        report.setCategory(roadCategory);

        AiTriageResultDTO result = aiTriageService.evaluate(report, null);

        assertThat(result.getDecision()).isEqualTo(AiTriageDecision.FLAGGED_FOR_REVIEW);
        assertThat(result.getImageQuality()).isEqualTo(AiImageQuality.NO_IMAGE);
        assertThat(result.getFlags()).contains("NO_IMAGE");
        assertThat(result.getConfidenceScore()).isLessThan(85);
    }

    @Test
    void evaluate_withTinyImage_flagsForReviewWithLowResolutionTag() {
        Report report = new Report();
        report.setReportCode("RPT-AI-002");
        report.setTitle("Pothole on Main Street");
        report.setDescription("A deep pothole in the left lane");
        report.setCategory(roadCategory);

        byte[] smallBytes = new byte[100]; // 100 bytes is far below 4KB
        MockMultipartFile file = new MockMultipartFile("images", "tiny.jpg", "image/jpeg", smallBytes);

        AiTriageResultDTO result = aiTriageService.evaluate(report, List.of(file));

        assertThat(result.getDecision()).isEqualTo(AiTriageDecision.FLAGGED_FOR_REVIEW);
        assertThat(result.getImageQuality()).isEqualTo(AiImageQuality.LOW_RESOLUTION);
        assertThat(result.getFlags()).contains("LOW_RESOLUTION");
    }

    @Test
    void evaluate_withBlurryOrUniformImage_flagsForReviewWithBlurryTag() throws IOException {
        Report report = new Report();
        report.setReportCode("RPT-AI-003");
        report.setTitle("Pothole on Main Street");
        report.setDescription("A deep pothole in the left lane");
        report.setCategory(roadCategory);

        // Create a solid pitch black image (flat, 0 variance)
        BufferedImage img = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] darkBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile("images", "dark.jpg", "image/jpeg", darkBytes);

        AiTriageResultDTO result = aiTriageService.evaluate(report, List.of(file));

        assertThat(result.getDecision()).isEqualTo(AiTriageDecision.FLAGGED_FOR_REVIEW);
        assertThat(result.getImageQuality()).isEqualTo(AiImageQuality.BLURRY);
        assertThat(result.getFlags()).contains("BLURRY_PHOTO");
    }

    @Test
    void evaluate_withUrgentSafetyHazard_escalatesForReviewWithHighRiskTag() throws IOException {
        Report report = new Report();
        report.setReportCode("RPT-AI-004");
        report.setTitle("Dangerous exposed wire sparking on street");
        report.setDescription("There is sparking and electric shock risk near the sidewalk");
        report.setCategory(roadCategory);

        MockMultipartFile file = createDetailedImageFile("spark.jpg");

        AiTriageResultDTO result = aiTriageService.evaluate(report, List.of(file));

        assertThat(result.getDecision()).isEqualTo(AiTriageDecision.FLAGGED_FOR_REVIEW);
        assertThat(result.getFlags()).contains("HIGH_RISK_HAZARD");
        assertThat(result.getReasoning()).contains("safety hazard");
    }

    @Test
    void evaluate_withDescriptionMismatch_flagsForReviewWithMismatchTag() throws IOException {
        Report report = new Report();
        report.setReportCode("RPT-AI-005");
        report.setTitle("Broken streetlight dark at night");
        report.setDescription("Streetlight bulb and lantern broken with blackout on the street");
        report.setCategory(roadCategory); // Tagged as Road, but text is Lighting

        MockMultipartFile file = createDetailedImageFile("mismatch.jpg");

        AiTriageResultDTO result = aiTriageService.evaluate(report, List.of(file));

        assertThat(result.getDecision()).isEqualTo(AiTriageDecision.FLAGGED_FOR_REVIEW);
        assertThat(result.getImageQuality()).isEqualTo(AiImageQuality.MISMATCH);
        assertThat(result.getFlags()).contains("DESCRIPTION_MISMATCH");
    }

    @Test
    void evaluate_withClearImageAndMatchingDescription_autoRoutesDirectlyToDepartment() throws IOException {
        Report report = new Report();
        report.setReportCode("RPT-AI-006");
        report.setTitle("Large pothole damaging road pavement");
        report.setDescription("Deep asphalt crater in the center lane causing severe traffic slowdown on Main Street");
        report.setCategory(roadCategory);

        MockMultipartFile file = createDetailedImageFile("clear_pothole.jpg");

        AiTriageResultDTO result = aiTriageService.evaluate(report, List.of(file));

        assertThat(result.getDecision()).isEqualTo(AiTriageDecision.AUTO_ROUTED);
        assertThat(result.getImageQuality()).isEqualTo(AiImageQuality.CLEAR);
        assertThat(result.getConfidenceScore()).isGreaterThanOrEqualTo(85);
        assertThat(result.getSuggestedDepartmentId()).isEqualTo(roadDepartment.getId());
        assertThat(result.getFlags()).contains("VERIFIED_INCIDENT", "AUTO_ROUTED_FAST_TRACK");
        assertThat(result.getReasoning()).contains("Auto-approved and routed directly to Roads & Bridges Department");
    }

    private MockMultipartFile createDetailedImageFile(String filename) throws IOException {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        // Draw varied shapes and contrasting colors to ensure high detail and variance
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, 400, 400);
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillOval(50, 50, 200, 200);
        g2d.setColor(Color.YELLOW);
        g2d.drawString("Municipal Incident Marker", 60, 150);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(20, 250, 360, 80);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] bytes = baos.toByteArray();
        return new MockMultipartFile("images", filename, "image/jpeg", bytes);
    }
}
