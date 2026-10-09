package com.hireshield.rules;

import java.util.Optional;

/**
 * One red flag check. Points and titles live in the rule_config table so an admin
 * can tune them without a redeploy; a rule only decides whether it matched and why.
 */
public interface RedFlagRule {

    String code();

    /** Returns the piece of text that triggered the rule, or empty when it did not match. */
    Optional<String> findEvidence(AnalysisContext context);
}
