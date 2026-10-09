package com.hireshield.rules;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class LookalikeDomainRule implements RedFlagRule {

    @Override
    public String code() {
        return "LOOKALIKE_DOMAIN";
    }

    @Override
    public Optional<String> findEvidence(AnalysisContext context) {
        return context.domains().values().stream()
                .filter(report -> report.lookalikeOf() != null)
                .findFirst()
                .map(report -> report.domain() + " looks like " + report.lookalikeOf() + " but is not the real site");
    }
}
