package com.hireshield.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import com.hireshield.entity.RiskLevel;
import com.hireshield.ml.MlResult.TermContribution;
import com.hireshield.rules.RuleHit;

class ExplanationServiceTest {

    @SuppressWarnings("unchecked")
    private final ExplanationService service =
            new ExplanationService(mock(ObjectProvider.class), false);

    @Test
    void usesTemplateWhenLlmIsOff() {
        var hits = List.of(new RuleHit("FEE_REQUEST", "Asks you to pay money", 30, "pay Rs 1999 registration fee"));
        var result = service.explain(RiskLevel.HIGH, 88, hits, List.of(new TermContribution("data entry", 0.4)),
                List.of(), "en");

        assertThat(result.source()).isEqualTo(ExplanationService.SOURCE_TEMPLATE);
        assertThat(result.text())
                .startsWith("This looks like a scam.")
                .contains("asks you to pay money")
                .contains("Real companies never ask you to pay")
                .contains("1930");
    }

    @Test
    void lowRiskStillGivesAdvice() {
        var result = service.explain(RiskLevel.LOW, 8, List.of(), List.of(), List.of(), "en");
        assertThat(result.text()).contains("did not find strong signs").contains("never pay money");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fallsBackWhenLlmFails() {
        ObjectProvider<ChatClient.Builder> provider = mock(ObjectProvider.class);
        // getIfAvailable returns null, as if no model was configured
        var enabled = new ExplanationService(provider, true);
        var result = enabled.explain(RiskLevel.CAUTION, 40, List.of(), List.of(), List.of("x.in was created only 10 days ago"), "en");
        assertThat(result.source()).isEqualTo(ExplanationService.SOURCE_TEMPLATE);
        assertThat(result.text()).contains("created only 10 days ago");
    }
}
