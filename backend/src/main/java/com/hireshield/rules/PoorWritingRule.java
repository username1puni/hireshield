package com.hireshield.rules;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hireshield.util.TextPatterns;

import org.springframework.stereotype.Component;

@Component
public class PoorWritingRule implements RedFlagRule {

    private static final Pattern MANY_BANGS = Pattern.compile("!{3,}");

    @Override
    public String code() {
        return "POOR_WRITING";
    }

    @Override
    public Optional<String> findEvidence(AnalysisContext context) {
        String text = context.text();
        int letters = 0;
        int upper = 0;
        int emojis = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (Character.isLetter(cp)) {
                letters++;
                if (Character.isUpperCase(cp)) {
                    upper++;
                }
            } else if (isEmoji(cp)) {
                emojis++;
            }
            i += Character.charCount(cp);
        }
        if (letters >= 40 && upper > letters * 0.4) {
            return Optional.of("Most of the message is in CAPITAL letters");
        }
        if (emojis >= 5) {
            return Optional.of(emojis + " emojis in one job message");
        }
        Matcher bangs = MANY_BANGS.matcher(text);
        if (bangs.find()) {
            return Optional.of(TextPatterns.snippet(text, bangs.start(), bangs.end()));
        }
        return Optional.empty();
    }

    private static boolean isEmoji(int cp) {
        return (cp >= 0x1F300 && cp <= 0x1FAFF) || (cp >= 0x2600 && cp <= 0x27BF);
    }
}
