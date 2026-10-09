package com.hireshield.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hireshield.dto.DomainReport;
import com.hireshield.dto.LinkReport;
import com.hireshield.entity.RiskLevel;

class ScoringServiceTest {

    private final ScoringService scoring = new ScoringService();

    @Test
    void cleanMessageIsLowRisk() {
        var score = scoring.combine(0, 0.05, 0);
        assertThat(score.finalScore()).isLessThan(30);
        assertThat(score.riskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void obviousFeeScamIsHighEvenWithoutWebsite() {
        // FEE_REQUEST + CHAT_ONLY + URGENCY = 55 rule points, nothing for the trust checks to look at
        var score = scoring.combine(55, 0.30, 0);
        assertThat(score.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void oneWeakSignalIsOnlyCaution() {
        var score = scoring.combine(10, 0.40, 0);
        assertThat(score.riskLevel()).isEqualTo(RiskLevel.CAUTION);
    }

    @Test
    void scoreNeverPassesHundred() {
        assertThat(scoring.combine(100, 1.0, 100).finalScore()).isLessThanOrEqualTo(100);
    }

    @Test
    void trustScoreAddsUpProblems() {
        DomainReport newLookalike = new DomainReport("tcs-jobs.in", null, 12, null, false, false, "tcs.com", false);
        DomainReport gmail = new DomainReport("gmail.com", null, null, null, null, true, null, false);
        LinkReport dangerous = new LinkReport("http://1.2.3.4/x", "1.2.3.4", LinkReport.LinkVerdict.DANGEROUS, List.of());

        assertThat(scoring.trustScore(List.of(gmail), List.of(), 0)).isZero();
        assertThat(scoring.trustScore(List.of(newLookalike), List.of(), 0)).isEqualTo(85);
        assertThat(scoring.trustScore(List.of(), List.of(dangerous), 1)).isEqualTo(100);
    }
}
