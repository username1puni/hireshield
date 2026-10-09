package com.hireshield.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.hireshield.entity.ReportStatus;
import com.hireshield.entity.ScamReport;

public record ScamReportResponse(Long id, String companyName, String email, String phone, String upiId,
                                 String domain, String platform, BigDecimal amountLost, String description,
                                 ReportStatus status, Instant createdAt) {

    public static ScamReportResponse from(ScamReport r) {
        return new ScamReportResponse(r.getId(), r.getCompanyName(), r.getEmail(), r.getPhone(), r.getUpiId(),
                r.getDomain(), r.getPlatform(), r.getAmountLost(), r.getDescription(), r.getStatus(), r.getCreatedAt());
    }
}
