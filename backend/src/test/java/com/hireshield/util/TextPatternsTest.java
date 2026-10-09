package com.hireshield.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextPatternsTest {

    private static final String SAMPLE = """
            Contact hr.jobs@gmail.com or call +91 98765 43210.
            Pay to rahul123@okaxis and visit https://bit.ly/abc or infosys-careers.co/apply.
            """;

    @Test
    void extractsEmailsWithoutUpiIds() {
        assertThat(TextPatterns.emails(SAMPLE)).containsExactly("hr.jobs@gmail.com");
    }

    @Test
    void extractsUpiIds() {
        assertThat(TextPatterns.upiIds(SAMPLE)).containsExactly("rahul123@okaxis");
        assertThat(TextPatterns.upiIds("mail me at a@ybl.com")).isEmpty();
    }

    @Test
    void extractsLinksButNotEmailParts() {
        assertThat(TextPatterns.urls(SAMPLE)).containsExactly("https://bit.ly/abc", "infosys-careers.co/apply");
    }

    @Test
    void extractsIndianPhoneNumbers() {
        assertThat(TextPatterns.phones(SAMPLE)).containsExactly("+919876543210");
    }

    @Test
    void domainOfHandlesUrlsAndEmails() {
        assertThat(TextPatterns.domainOf("https://www.Example.com/jobs?id=1")).isEqualTo("example.com");
        assertThat(TextPatterns.domainOf("someone@Mail.Company.in")).isEqualTo("mail.company.in");
        assertThat(TextPatterns.domainOf("site.com:8080/x")).isEqualTo("site.com");
    }

    @Test
    void parsesIndianAmounts() {
        assertThat(TextPatterns.parseAmount("5,000", null)).isEqualTo(5000);
        assertThat(TextPatterns.parseAmount("25", "k")).isEqualTo(25_000);
        assertThat(TextPatterns.parseAmount("2.5", "lakh")).isEqualTo(250_000);
        assertThat(TextPatterns.parseAmount("abc", null)).isEqualTo(-1);
    }
}
