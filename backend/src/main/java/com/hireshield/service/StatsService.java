package com.hireshield.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hireshield.dto.PublicStatsResponse;
import com.hireshield.dto.UserStatsResponse;
import com.hireshield.entity.ReportStatus;
import com.hireshield.entity.RiskLevel;
import com.hireshield.repository.AnalysisRepository;
import com.hireshield.repository.BlacklistEntryRepository;
import com.hireshield.repository.FeedbackRepository;
import com.hireshield.repository.ScamReportRepository;
import com.hireshield.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private final AnalysisRepository analysisRepository;
    private final ScamReportRepository reportRepository;
    private final BlacklistEntryRepository blacklistRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PublicStatsResponse publicStats() {
        Instant startOfDay = LocalDate.now(INDIA).atStartOfDay(INDIA).toInstant();
        return new PublicStatsResponse(
                analysisRepository.count(),
                analysisRepository.countByRiskLevel(RiskLevel.HIGH),
                analysisRepository.countByCreatedAtAfter(startOfDay),
                reportRepository.countByStatus(ReportStatus.APPROVED),
                blacklistRepository.count());
    }

    @Transactional(readOnly = true)
    public UserStatsResponse userStats(Long userId) {
        Map<RiskLevel, Long> byRisk = new EnumMap<>(RiskLevel.class);
        for (RiskLevel level : RiskLevel.values()) {
            byRisk.put(level, 0L);
        }
        long total = 0;
        for (Object[] row : analysisRepository.countByRiskForUser(userId)) {
            long count = ((Number) row[1]).longValue();
            byRisk.put((RiskLevel) row[0], count);
            total += count;
        }
        return new UserStatsResponse(total, byRisk);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> usage() {
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("users", userRepository.count());
        usage.put("checks", analysisRepository.count());
        usage.put("highRisk", analysisRepository.countByRiskLevel(RiskLevel.HIGH));
        usage.put("pendingReports", reportRepository.countByStatus(ReportStatus.PENDING));
        usage.put("feedbackCorrect", feedbackRepository.countByCorrect(true));
        usage.put("feedbackWrong", feedbackRepository.countByCorrect(false));
        Map<String, Long> flags = new LinkedHashMap<>();
        for (Object[] row : analysisRepository.countFlagsByRule()) {
            flags.put((String) row[0], ((Number) row[1]).longValue());
        }
        usage.put("flagsByRule", flags);
        return usage;
    }
}
