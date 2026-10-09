package com.hireshield.rules;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class NewDomainRule implements RedFlagRule {

    static final int NEW_DOMAIN_DAYS = 180;

    @Override
    public String code() {
        return "NEW_DOMAIN";
    }

    @Override
    public Optional<String> findEvidence(AnalysisContext context) {
        return context.domains().values().stream()
                .filter(report -> !report.freeEmailProvider() && report.isNew(NEW_DOMAIN_DAYS))
                .findFirst()
                .map(report -> report.domain() + " was registered only " + report.ageDays() + " days ago");
    }
}
