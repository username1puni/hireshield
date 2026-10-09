package com.hireshield.service;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hireshield.dto.PageResponse;
import com.hireshield.dto.ReportReviewRequest;
import com.hireshield.dto.ScamReportRequest;
import com.hireshield.dto.ScamReportResponse;
import com.hireshield.entity.ReportStatus;
import com.hireshield.entity.ScamReport;
import com.hireshield.exception.BadRequestException;
import com.hireshield.exception.NotFoundException;
import com.hireshield.repository.ScamReportRepository;
import com.hireshield.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ScamReportRepository reportRepository;
    private final UserRepository userRepository;
    private final BlacklistService blacklistService;

    @Transactional
    public ScamReportResponse create(Long userId, ScamReportRequest request) {
        ScamReport report = new ScamReport();
        report.setReporter(userRepository.getReferenceById(userId));
        report.setCompanyName(trim(request.companyName()));
        report.setEmail(trim(request.email()));
        report.setPhone(trim(request.phone()));
        report.setUpiId(trim(request.upiId()));
        report.setDomain(trim(request.domain()));
        report.setPlatform(trim(request.platform()));
        report.setAmountLost(request.amountLost());
        report.setDescription(request.description().trim());
        return ScamReportResponse.from(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public PageResponse<ScamReportResponse> byStatus(ReportStatus status, int page, int size) {
        return PageResponse.from(reportRepository
                .findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, Math.min(size, 50)))
                .map(ScamReportResponse::from));
    }

    @Transactional
    public ScamReportResponse review(Long id, ReportReviewRequest request) {
        if (request.status() == ReportStatus.PENDING) {
            throw new BadRequestException("Status must be APPROVED or REJECTED");
        }
        ScamReport report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report " + id + " not found"));
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BadRequestException("This report was already reviewed");
        }
        report.setStatus(request.status());
        report.setReviewedAt(Instant.now());
        if (request.status() == ReportStatus.APPROVED) {
            blacklistService.addFromReport(report);
        }
        return ScamReportResponse.from(report);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
