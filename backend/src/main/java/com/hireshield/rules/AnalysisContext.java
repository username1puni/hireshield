package com.hireshield.rules;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.hireshield.dto.DomainReport;
import com.hireshield.util.TextPatterns;

/**
 * Everything the rules need about one message, extracted once.
 */
public record AnalysisContext(
        String text,
        String lowerText,
        List<String> emails,
        List<String> urls,
        List<String> phones,
        List<String> upiIds,
        Map<String, DomainReport> domains) {

    public static AnalysisContext of(String text, Map<String, DomainReport> domains) {
        return new AnalysisContext(
                text,
                text.toLowerCase(Locale.ROOT),
                TextPatterns.emails(text),
                TextPatterns.urls(text),
                TextPatterns.phones(text),
                TextPatterns.upiIds(text),
                domains);
    }

    public static AnalysisContext of(String text) {
        return of(text, Map.of());
    }
}
