package com.hireshield.rules;

import org.springframework.stereotype.Component;

@Component
public class UrgencyRule extends PatternRule {

    public UrgencyRule() {
        super(
                "\\blimited\\s+(seats?|slots?|vacanc(y|ies)|positions?|offer)\\b",
                "\\b(join|apply|register|respond|reply|confirm|pay)\\s+(today|now|immediately|asap)\\b",
                "\\bwithin\\s+(24|48|12|2|6)\\s*(hours?|hrs?)\\b",
                "\\b(only|last)\\s+(today|\\d+\\s+seats?|few\\s+seats?|few\\s+slots?)\\b",
                "\\b(hurry|act\\s+fast|act\\s+now|don'?t\\s+miss|offer\\s+expires?|expires?\\s+today|first\\s+come\\s+first\\s+serve)",
                "\\bimmediate\\s+joining\\b[^.\\n]{0,40}\\b(fee|pay|deposit)\\b");
    }

    @Override
    public String code() {
        return "URGENCY";
    }
}
