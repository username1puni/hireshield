package com.hireshield.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hireshield.entity.RiskLevel;
import com.hireshield.ml.MlResult.TermContribution;
import com.hireshield.rules.RuleHit;

import lombok.extern.slf4j.Slf4j;

/**
 * Writes the short "why" paragraph shown under the score.
 *
 * <p>If an LLM key is configured (LLM_ENABLED=true plus LLM_API_KEY), the facts are sent to the
 * model to be rewritten in friendly words. Otherwise, or if that call fails, a template
 * built from the same facts is used, so the app never depends on the LLM.
 */
@Slf4j
@Service
public class ExplanationService {

    public static final String SOURCE_TEMPLATE = "TEMPLATE";
    public static final String SOURCE_LLM = "LLM";

    private static final String SYSTEM_PROMPT = """
            You help Indian students and freshers spot fake job and internship offers.
            You are given the result of an automatic check. Explain it in 3 to 5 short sentences of simple English.
            Only use the facts given. Do not invent company details. If the risk is high, end with one clear
            next step (for example: do not pay, verify on the company's official careers page, report on
            cybercrime.gov.in or call 1930). Do not use markdown.""";

    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;
    private final boolean llmEnabled;

    public ExplanationService(ObjectProvider<ChatClient.Builder> chatClientBuilder,
                              @Value("${app.llm.enabled:false}") boolean llmEnabled) {
        this.chatClientBuilder = chatClientBuilder;
        this.llmEnabled = llmEnabled;
    }

    public record Explanation(String text, String source) {
    }

    public Explanation explain(RiskLevel risk, int score, List<RuleHit> hits, List<TermContribution> terms,
                               List<String> trustNotes, String language) {
        String facts = facts(risk, score, hits, terms, trustNotes);
        if (llmEnabled) {
            try {
                ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
                if (builder != null) {
                    String answer = builder.build().prompt()
                            .system(SYSTEM_PROMPT)
                            .user("Answer in " + ("hi".equalsIgnoreCase(language) ? "Hindi" : "English") + ".\n\n" + facts)
                            .call()
                            .content();
                    if (answer != null && !answer.isBlank()) {
                        return new Explanation(answer.trim(), SOURCE_LLM);
                    }
                }
            } catch (RuntimeException e) {
                log.warn("LLM explanation failed, using template: {}", e.getMessage());
            }
        }
        return new Explanation(template(risk, hits, terms, trustNotes), SOURCE_TEMPLATE);
    }

    String template(RiskLevel risk, List<RuleHit> hits, List<TermContribution> terms, List<String> trustNotes) {
        StringBuilder text = new StringBuilder();
        switch (risk) {
            case HIGH -> text.append("This looks like a scam. ");
            case CAUTION -> text.append("Be careful with this offer. ");
            case LOW -> text.append("We did not find strong signs of a scam. ");
        }
        if (!hits.isEmpty()) {
            String reasons = hits.stream().limit(3)
                    .map(h -> h.title().toLowerCase())
                    .collect(Collectors.joining(", "));
            text.append("Main warning signs: ").append(reasons).append(". ");
        }
        if (!trustNotes.isEmpty()) {
            text.append(trustNotes.get(0)).append(". ");
        }
        if (!terms.isEmpty() && risk != RiskLevel.LOW) {
            text.append("Our model also noticed words that are common in fake job posts, like \"")
                    .append(terms.stream().limit(3).map(TermContribution::term).collect(Collectors.joining("\", \"")))
                    .append("\". ");
        }
        boolean asksMoney = hits.stream().anyMatch(h -> h.code().equals("FEE_REQUEST") || h.code().equals("TASK_SCAM"));
        if (asksMoney) {
            text.append("Real companies never ask you to pay to get a job. ");
        }
        switch (risk) {
            case HIGH -> text.append("Do not pay or share personal details. You can report it at cybercrime.gov.in or call 1930.");
            case CAUTION -> text.append("Check the company's official careers page and talk to a real HR person before you go ahead.");
            case LOW -> text.append("Still, never pay money for a job and verify the company on its official website.");
        }
        return text.toString().trim();
    }

    private static String facts(RiskLevel risk, int score, List<RuleHit> hits, List<TermContribution> terms,
                                List<String> trustNotes) {
        StringBuilder facts = new StringBuilder();
        facts.append("Scam score: ").append(score).append("/100 (").append(risk).append(")\n");
        facts.append("Red flags found:\n");
        if (hits.isEmpty()) {
            facts.append("- none\n");
        }
        for (RuleHit hit : hits) {
            facts.append("- ").append(hit.title()).append(": \"").append(hit.evidence()).append("\"\n");
        }
        if (!trustNotes.isEmpty()) {
            facts.append("Website and contact checks:\n");
            trustNotes.forEach(note -> facts.append("- ").append(note).append('\n'));
        }
        if (!terms.isEmpty()) {
            facts.append("Words the ML model found suspicious: ")
                    .append(terms.stream().map(TermContribution::term).collect(Collectors.joining(", ")))
                    .append('\n');
        }
        return facts.toString();
    }
}
