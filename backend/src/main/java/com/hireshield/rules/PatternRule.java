package com.hireshield.rules;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hireshield.util.TextPatterns;

/**
 * Base class for rules that are just a list of phrases.
 */
public abstract class PatternRule implements RedFlagRule {

    private final List<Pattern> patterns;

    protected PatternRule(String... regexes) {
        this.patterns = java.util.Arrays.stream(regexes)
                .map(r -> Pattern.compile(r, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE))
                .toList();
    }

    @Override
    public Optional<String> findEvidence(AnalysisContext context) {
        return firstMatch(context.text());
    }

    protected Optional<String> firstMatch(String text) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return Optional.of(TextPatterns.snippet(text, matcher.start(), matcher.end()));
            }
        }
        return Optional.empty();
    }
}
