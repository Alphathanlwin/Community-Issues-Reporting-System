package com.uit.scirs.common.util;

import com.uit.scirs.report.repository.ReportRepository;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
public class ReportCodeGenerator {

    private final ReportRepository reportRepository;

    public ReportCodeGenerator(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public synchronized String next() {
        int year = Year.now().getValue();
        long sequence = reportRepository.count() + 1;
        String code = format(year, sequence);

        while (reportRepository.existsByReportCode(code)) {
            sequence++;
            code = format(year, sequence);
        }

        return code;
    }

    private String format(int year, long sequence) {
        return String.format("RPT-%d-%06d", year, sequence);
    }
}
