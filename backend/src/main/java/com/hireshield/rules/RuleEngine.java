package com.hireshield.rules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.hireshield.entity.RuleConfig;
import com.hireshield.repository.RuleConfigRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Runs every red flag rule and adds up the points (capped at 100).
 * Rule settings are cached in memory and refreshed when an admin edits them.
 */
@Slf4j
@Service
public class RuleEngine {

    private static final int MAX_SCORE = 100;

    private final List<RedFlagRule> rules;
    private final RuleConfigRepository ruleConfigRepository;
    private final Map<String, RuleConfig> configCache = new ConcurrentHashMap<>();

    public RuleEngine(List<RedFlagRule> rules, RuleConfigRepository ruleConfigRepository) {
        this.rules = rules;
        this.ruleConfigRepository = ruleConfigRepository;
    }

    public List<RuleHit> evaluate(AnalysisContext context) {
        if (configCache.isEmpty()) {
            reloadConfig();
        }
        List<RuleHit> hits = new ArrayList<>();
        for (RedFlagRule rule : rules) {
            RuleConfig config = configCache.get(rule.code());
            if (config == null || !config.isEnabled()) {
                continue;
            }
            Optional<String> evidence;
            try {
                evidence = rule.findEvidence(context);
            } catch (RuntimeException e) {
                // One broken rule should not fail the whole check.
                log.warn("Rule {} failed: {}", rule.code(), e.getMessage());
                continue;
            }
            evidence.ifPresent(text -> hits.add(new RuleHit(rule.code(), config.getTitle(), config.getPoints(), text)));
        }
        hits.sort(Comparator.comparingInt(RuleHit::points).reversed());
        return hits;
    }

    public static int score(List<RuleHit> hits) {
        return Math.min(MAX_SCORE, hits.stream().mapToInt(RuleHit::points).sum());
    }

    public void reloadConfig() {
        configCache.clear();
        ruleConfigRepository.findAll().forEach(config -> configCache.put(config.getCode(), config));
    }
}
