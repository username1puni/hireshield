package com.hireshield.dto;


public record PublicStatsResponse(long totalChecks, long highRiskFound, long checksToday, long approvedReports,
                                  long reportedIdentifiers) {
}
