package com.hireshield.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hireshield.dto.DomainReport;
import com.hireshield.dto.LinkReport;
import com.hireshield.entity.RiskLevel;

/**
 * Combines the three signals into one 0-100 score.
 *
 * <p>The plan started with a weighted average (0.40 ML + 0.35 rules + 0.25 trust). In testing,
 * an obvious fee scam with no website scored only ~30 because the trust signal had nothing
 * to look at and pulled the average down. So the signals are combined like independent
 * pieces of evidence instead: score = 1 - (1 - wM*M)(1 - wR*R)(1 - wT*T). One strong signal
 * is enough to raise the score, and a missing signal does not lower it.
 */
@Service
public class ScoringService {

    static final double ML_WEIGHT = 0.75;
    static final double RULE_WEIGHT = 0.95;
    static final double TRUST_WEIGHT = 0.85;

    public record Score(int finalScore, int ruleScore, double mlProbability, int trustScore, RiskLevel riskLevel) {
    }

    public Score combine(int ruleScore, double mlProbability, int trustScore) {
        double m = clamp(mlProbability);
        double r = clamp(ruleScore / 100.0);
        double t = clamp(trustScore / 100.0);
        double combined = 1 - (1 - ML_WEIGHT * m) * (1 - RULE_WEIGHT * r) * (1 - TRUST_WEIGHT * t);
        int score = (int) Math.round(combined * 100);
        return new Score(score, ruleScore, mlProbability, trustScore, RiskLevel.fromScore(score));
    }

    /**
     * Trust score: problems with the websites, emails and links found in the message,
     * plus any identifier already reported by other users.
     */
    public int trustScore(List<DomainReport> domains, List<LinkReport> links, int blacklistHits) {
        int score = 0;
        for (DomainReport d : domains) {
            if (d.freeEmailProvider()) {
                continue;
            }
            if (d.isNew(180)) {
                score += 35;
            }
            if (Boolean.FALSE.equals(d.hasMx())) {
                score += 15;
            }
            if (d.lookalikeOf() != null) {
                score += 35;
            }
        }
        for (LinkReport link : links) {
            score += switch (link.verdict()) {
                case DANGEROUS -> 45;
                case SUSPICIOUS -> 10;
                case SAFE -> 0;
            };
        }
        score += blacklistHits * 60;
        return Math.min(100, score);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
