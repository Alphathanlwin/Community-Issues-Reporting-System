package com.uit.scirs.report.repository;

import com.uit.scirs.report.entity.ImageType;
import com.uit.scirs.report.entity.ReportImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportImageRepository extends JpaRepository<ReportImage, Long> {

    List<ReportImage> findByReportId(Long reportId);

    List<ReportImage> findByReportIdAndImageType(Long reportId, ImageType imageType);
}
