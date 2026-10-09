package com.hireshield.dto;

import jakarta.validation.constraints.NotNull;

import com.hireshield.entity.ReportStatus;

public record ReportReviewRequest(@NotNull ReportStatus status) {
}
