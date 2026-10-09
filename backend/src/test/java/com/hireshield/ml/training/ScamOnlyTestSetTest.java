package com.hireshield.ml.training;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.hireshield.ml.TextNormalizer;

class ScamOnlyTestSetTest {

    @Test
    void mapsAnonymisedTagsToTheSameTokensAsEmscad() {
        String text = ScamOnlyTestSet.mapTags("Earn 3000 daily, add <PHONE_NUMBER> or visit <URL> - <NAMED_ENTITY> HR");

        String normalized = TextNormalizer.normalize(text);

        assertThat(normalized).contains("phonetoken").contains("urltoken");
        assertThat(normalized).doesNotContain("named").doesNotContain("entity");
    }

    @Test
    void dropsTagsWithoutAnEquivalent() {
        assertThat(ScamOnlyTestSet.mapTags("Join on <DATE_TIME> at <LOCATION>").trim()).isEqualTo("Join on   at");
    }
}
