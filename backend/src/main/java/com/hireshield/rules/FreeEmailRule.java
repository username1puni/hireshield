package com.hireshield.rules;

import java.util.Optional;
import java.util.regex.Pattern;

import com.hireshield.util.TextPatterns;

import org.springframework.stereotype.Component;

@Component
public class FreeEmailRule implements RedFlagRule {

    private static final Pattern HIRING_CONTEXT = Pattern.compile(
            "\\b(hr|recruit\\w*|hiring|job|internship|offer|vacancy|career|interview|selected|joining|resume|cv)\\b",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String code() {
        return "FREE_EMAIL";
    }

    @Override
    public Optional<String> findEvidence(AnalysisContext context) {
        if (!HIRING_CONTEXT.matcher(context.text()).find()) {
            return Optional.empty();
        }
        return context.emails().stream()
                .filter(email -> FreeEmailProviders.isFree(TextPatterns.domainOf(email)))
                .findFirst()
                .map(email -> email + " (free email service, not a company domain)");
    }
}
