package com.hireshield.rules;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hireshield.util.TextPatterns;

import org.springframework.stereotype.Component;

/**
 * Flags pay that is far above normal for the kind of work, for example
 * "earn Rs 5,000 per day" for data entry from home.
 */
@Component
public class HighSalaryRule implements RedFlagRule {

    private static final Pattern RATE = Pattern.compile(
            "(₹|rs\\.?|inr)\\s?(\\d[\\d,]*(\\.\\d+)?)\\s?(k)?\\s*(/|per|a|every|in\\s+a|daily)?\\s*(-|/)?\\s*(hour|hr|day|daily|week|weekly|month|monthly)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EASY_WORK = Pattern.compile(
            "\\b(data\\s+entry|typing|copy\\s+paste|part[\\s-]?time|work\\s+from\\s+home|ghar\\s+baithe|no\\s+experience|freshers?|students?|housewives|simple\\s+work|easy\\s+work|mobile\\s+work|form\\s+filling)\\b",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String code() {
        return "HIGH_SALARY";
    }

    @Override
    public Optional<String> findEvidence(AnalysisContext context) {
        String text = context.text();
        boolean easyWork = EASY_WORK.matcher(text).find();
        Matcher matcher = RATE.matcher(text);
        while (matcher.find()) {
            long amount = TextPatterns.parseAmount(matcher.group(2), matcher.group(4));
            String period = matcher.group(7) == null ? "" : matcher.group(7).toLowerCase(Locale.ROOT);
            if (period.isEmpty() && matcher.group(5) != null && matcher.group(5).equalsIgnoreCase("daily")) {
                period = "day";
            }
            if (isTooHigh(amount, period, easyWork)) {
                return Optional.of(TextPatterns.snippet(text, matcher.start(), matcher.end()));
            }
        }
        return Optional.empty();
    }

    static boolean isTooHigh(long amount, String period, boolean easyWork) {
        if (amount <= 0) {
            return false;
        }
        if (period.startsWith("h")) {
            return amount >= 800;
        }
        if (period.startsWith("d")) {
            return amount >= 3000 || (easyWork && amount >= 1500);
        }
        if (period.startsWith("w")) {
            return amount >= 20000 || (easyWork && amount >= 8000);
        }
        if (period.startsWith("m")) {
            // Big monthly pay is normal for experienced roles, so only flag it for easy work.
            return easyWork && amount >= 60000;
        }
        return false;
    }
}
