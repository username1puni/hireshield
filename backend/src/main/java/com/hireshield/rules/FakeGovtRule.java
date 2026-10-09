package com.hireshield.rules;

import java.util.Optional;

import com.hireshield.dto.DomainReport;
import com.hireshield.util.TextPatterns;

import org.springframework.stereotype.Component;

@Component
public class FakeGovtRule extends PatternRule {

    public FakeGovtRule() {
        super(
                "\\b(govt|government|sarkari|central\\s+govt|state\\s+govt)\\.?\\s+(job|jobs|naukri|vacancy|vacancies|recruitment|post|posts)\\b",
                "\\b(railway|indian\\s+railways?|rrb|ssc|india\\s+post|postal|ministry\\s+of\\s+\\w+|aiims|drdo|isro|fci|anganwadi)\\b[^.\\n]{0,40}\\b(recruitment|vacanc(y|ies)|bharti|jobs?)\\b");
    }

    @Override
    public String code() {
        return "FAKE_GOVT";
    }

    @Override
    public Optional<String> findEvidence(AnalysisContext context) {
        Optional<String> govtClaim = super.findEvidence(context);
        if (govtClaim.isEmpty()) {
            return Optional.empty();
        }
        // Real government recruitment happens on .gov.in / .nic.in sites and official emails.
        Optional<String> nonGovSource = context.domains().values().stream()
                .filter(report -> !report.governmentDomain())
                .map(DomainReport::domain)
                .findFirst();
        if (nonGovSource.isEmpty()) {
            nonGovSource = context.emails().stream()
                    .map(TextPatterns::domainOf)
                    .filter(domain -> !domain.endsWith(".gov.in") && !domain.endsWith(".nic.in"))
                    .findFirst();
        }
        return nonGovSource.map(domain -> govtClaim.get() + " (but the contact is " + domain + ", not a .gov.in site)");
    }
}
