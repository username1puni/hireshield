package com.hireshield.dto;

import java.time.Instant;
import java.util.List;

import com.hireshield.entity.InputType;
import com.hireshield.entity.RiskLevel;

public record AnalysisResponse(
        Long id,
        int score,
        RiskLevel riskLevel,
        int ruleScore,
        double mlProbability,
        int trustScore,
        List<FlagResponse> flags,
        List<String> suspiciousTerms,
        List<DomainReport> domains,
        List<LinkReport> links,
        List<String> reportedIdentifiers,
        String explanation,
        String explanationSource,
        InputType inputType,
        String text,
        String modelVersion,
        Instant createdAt) {
}
