package com.hireshield.rules;

import org.springframework.stereotype.Component;

@Component
public class PersonalDataRule extends PatternRule {

    public PersonalDataRule() {
        super(
                "\\b(send|share|provide|submit|tell|give|forward)\\b[^.\\n]{0,40}\\b(otp|one time password|aadhaa?r|pan\\s+card|pan\\s+number|bank\\s+(account|details|passbook)|atm\\s+(card|pin)|cvv|upi\\s+pin|net\\s?banking|card\\s+number)\\b",
                "\\b(otp|aadhaa?r|pan\\s+card|bank\\s+details|atm\\s+pin|cvv|upi\\s+pin)\\b[^.\\n]{0,30}\\b(for\\s+verification|to\\s+verify|required|mandatory|immediately)\\b");
    }

    @Override
    public String code() {
        return "PERSONAL_DATA";
    }
}
