package com.hireshield.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DomainServiceTest {

    @ParameterizedTest
    @CsvSource({
            "infosys-careers.co, infosys.com",
            "tcs-jobs.in, tcs.com",
            "wiproo.com, wipro.com",
            "amazonhiring.xyz, amazon.com",
            "flipkart-offer.online, flipkart.com"})
    void detectsLookalikes(String domain, String brand) {
        assertThat(DomainService.lookalikeOf(domain)).isEqualTo(brand);
    }

    @ParameterizedTest
    @CsvSource({"infosys.com", "careers.tcs.com", "amazon.in", "example.org", "gmail.com", "statics.com"})
    void leavesRealDomainsAlone(String domain) {
        assertThat(DomainService.lookalikeOf(domain)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "jobs.example.co.in, example.co.in",
            "a.b.example.com, example.com",
            "example.com, example.com"})
    void findsRegistrableDomain(String domain, String expected) {
        assertThat(DomainService.registrableDomain(domain)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"kitten, sitting, 3", "wipro, wlpro, 1", "same, same, 0"})
    void levenshteinDistance(String a, String b, int expected) {
        assertThat(DomainService.levenshtein(a, b)).isEqualTo(expected);
    }
}
