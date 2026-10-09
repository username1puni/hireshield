package com.hireshield.ml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class TfidfVectorizerTest {

    @Test
    void normalizerReplacesContactDetails() {
        String normalized = TextNormalizer.normalize("Mail <b>hr@x.com</b> or visit https://x.com, pay Rs 5,000");
        assertThat(normalized).isEqualTo("mail emailtoken or visit urltoken pay moneytoken");
    }

    @Test
    void termsIncludeBigrams() {
        assertThat(TextNormalizer.terms("Registration fee required"))
                .contains("registration", "fee", "registration fee", "fee required");
    }

    @Test
    void vectorsAreUnitLengthAndIgnoreUnknownWords() {
        List<List<String>> docs = List.of(
                TextNormalizer.terms("data entry job from home"),
                TextNormalizer.terms("java developer job in pune"),
                TextNormalizer.terms("data entry work, earn daily"));
        TfidfVectorizer vectorizer = TfidfVectorizer.fit(docs, 1, 1.0, 1000);

        var vector = vectorizer.transform("data entry job zebra");
        double length = Math.sqrt(vector.values().stream().mapToDouble(v -> v * v).sum());

        assertThat(length).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(vector).containsKeys("data", "entry", "data entry").doesNotContainKey("zebra");
    }

    @Test
    void maxFeaturesKeepsMostCommonTerms() {
        List<List<String>> docs = List.of(List.of("a1", "b1"), List.of("a1", "c1"), List.of("a1", "b1"));
        TfidfVectorizer vectorizer = TfidfVectorizer.fit(docs, 1, 1.0, 2);
        assertThat(vectorizer.vocabulary()).containsExactlyInAnyOrder("a1", "b1");
    }
}
