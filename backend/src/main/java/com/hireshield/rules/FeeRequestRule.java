package com.hireshield.rules;

import org.springframework.stereotype.Component;

@Component
public class FeeRequestRule extends PatternRule {

    public FeeRequestRule() {
        super(
                "\\b(registration|training|security|joining|processing|onboarding|verification|documentation|interview|application|kit|laptop|uniform|id card)\\s+(fee|fees|charge|charges|deposit|amount|cost)\\b",
                "\\brefundable\\s+(fee|deposit|amount)\\b",
                "\\b(pay|deposit|transfer|send)\\b[^.\\n]{0,40}\\b(to|for)\\s+(confirm|book|reserve|secure|block|activate)\\b",
                "\\b(pay|deposit|transfer)\\s+(only\\s+)?(₹|rs\\.?|inr)\\s?\\d",
                "\\b(fee|fees|deposit)\\b[^.\\n]{0,30}\\b(₹|rs\\.?|inr)\\s?\\d");
    }

    @Override
    public String code() {
        return "FEE_REQUEST";
    }
}
