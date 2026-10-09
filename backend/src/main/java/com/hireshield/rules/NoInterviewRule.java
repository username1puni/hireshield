package com.hireshield.rules;

import org.springframework.stereotype.Component;

@Component
public class NoInterviewRule extends PatternRule {

    public NoInterviewRule() {
        super(
                "\\b(no|without\\s+(any\\s+)?)\\s*interviews?\\b",
                "\\bdirect\\s+(selection|joining|offer|recruitment)\\b",
                "\\b(100\\s?%|guaranteed?)\\s+(job|placement|selection|income)\\b",
                "\\bjob\\s+guarantee\\b",
                "\\bselected\\s+without\\b",
                "\\bon\\s+the\\s+spot\\s+(offer|selection)\\b");
    }

    @Override
    public String code() {
        return "NO_INTERVIEW";
    }
}
