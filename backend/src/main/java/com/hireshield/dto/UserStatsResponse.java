package com.hireshield.dto;

import java.util.Map;

import com.hireshield.entity.RiskLevel;

public record UserStatsResponse(long total, Map<RiskLevel, Long> byRisk) {
}
