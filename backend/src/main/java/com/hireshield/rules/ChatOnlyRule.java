package com.hireshield.rules;

import org.springframework.stereotype.Component;

@Component
public class ChatOnlyRule extends PatternRule {

    public ChatOnlyRule() {
        super(
                "\\b(contact|message|msg|ping|dm|text|join|reach|chat|apply)\\b[^.\\n]{0,30}\\b(on|via|through|in)\\s+(whats\\s?app|telegram|wa)\\b",
                "\\b(whats\\s?app|telegram)\\s+(only|no\\.?|number|group|channel|id)\\b",
                "\\b(t\\.me|wa\\.me|chat\\.whatsapp\\.com)/\\S+");
    }

    @Override
    public java.util.Optional<String> findEvidence(AnalysisContext context) {
        // A chat link next to an official company email is common in real campus drives too.
        boolean hasCompanyEmail = context.emails().stream()
                .map(com.hireshield.util.TextPatterns::domainOf)
                .anyMatch(d -> !FreeEmailProviders.isFree(d));
        return hasCompanyEmail ? java.util.Optional.empty() : super.findEvidence(context);
    }

    @Override
    public String code() {
        return "CHAT_ONLY";
    }
}
