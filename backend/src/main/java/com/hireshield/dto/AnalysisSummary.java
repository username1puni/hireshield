package com.hireshield.dto;

import java.time.Instant;

import com.hireshield.entity.Analysis;
import com.hireshield.entity.InputType;
import com.hireshield.entity.RiskLevel;

public record AnalysisSummary(Long id, int score, RiskLevel riskLevel, InputType inputType, String preview,
                              int flagCount, Instant createdAt) {

    public static AnalysisSummary from(Analysis a) {
        String text = a.getRawText().replaceAll("\\s+", " ").trim();
        String preview = text.length() > 120 ? text.substring(0, 117) + "..." : text;
        return new AnalysisSummary(a.getId(), a.getFinalScore(), a.getRiskLevel(), a.getInputType(), preview,
                a.getFlags().size(), a.getCreatedAt());
    }
}
