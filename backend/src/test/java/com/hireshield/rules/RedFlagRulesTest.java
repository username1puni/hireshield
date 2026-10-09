package com.hireshield.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.hireshield.dto.DomainReport;

class RedFlagRulesTest {

    static Stream<Arguments> scamSamples() {
        return Stream.of(
                Arguments.of(new FeeRequestRule(), "Pay Rs 1999 registration fee to confirm your seat"),
                Arguments.of(new FeeRequestRule(), "A refundable security deposit of 2500 is required"),
                Arguments.of(new FeeRequestRule(), "Kindly transfer the amount to confirm your joining"),
                Arguments.of(new TaskScamRule(), "Just like YouTube videos and earn daily"),
                Arguments.of(new TaskScamRule(), "Rate hotels on Google Maps reviews, prepaid task bonus"),
                Arguments.of(new PersonalDataRule(), "Please share the OTP received for verification"),
                Arguments.of(new PersonalDataRule(), "Send your Aadhaar and bank details to HR"),
                Arguments.of(new ChatOnlyRule(), "Contact HR on WhatsApp 98765 43210 for joining"),
                Arguments.of(new ChatOnlyRule(), "Join here t.me/jobsfast"),
                Arguments.of(new UrgencyRule(), "Limited seats, join today"),
                Arguments.of(new UrgencyRule(), "Reply within 24 hours or the offer is cancelled"),
                Arguments.of(new NoInterviewRule(), "Direct selection, no interview required"),
                Arguments.of(new NoInterviewRule(), "100% job guarantee after training"),
                Arguments.of(new HighSalaryRule(), "Data entry work, earn Rs 4000 per day"),
                Arguments.of(new HighSalaryRule(), "Part time job. Salary ₹25,000 per week"),
                Arguments.of(new PoorWritingRule(), "URGENT HIRING!!! WORK FROM HOME AND EARN BIG MONEY NOW APPLY"),
                Arguments.of(new FakeGovtRule(), "Railway recruitment 2026, apply at rrb-jobs.com or mail rrb.help@gmail.com"));
    }

    static Stream<Arguments> safeSamples() {
        return Stream.of(
                Arguments.of(new FeeRequestRule(), "There is no fee at any stage of our recruitment process"),
                Arguments.of(new TaskScamRule(), "You will review pull requests and write unit tests"),
                Arguments.of(new PersonalDataRule(), "Bring your college ID card to the interview"),
                Arguments.of(new ChatOnlyRule(), "Questions? Write to careers@infosys.com or WhatsApp us on 98765 43210"),
                Arguments.of(new UrgencyRule(), "Applications close on 30 November"),
                Arguments.of(new NoInterviewRule(), "Selection: online test, technical interview and HR interview"),
                Arguments.of(new HighSalaryRule(), "CTC Rs 12 LPA for software engineers with 3 years experience"),
                Arguments.of(new PoorWritingRule(), "We are hiring Java developers. Apply on our careers page."),
                Arguments.of(new FakeGovtRule(), "Railway recruitment notice published on indianrailways.gov.in"));
    }

    @ParameterizedTest
    @MethodSource("scamSamples")
    void flagsScamText(RedFlagRule rule, String text) {
        assertThat(rule.findEvidence(contextFor(text))).as(rule.code() + ": " + text).isPresent();
    }

    @ParameterizedTest
    @MethodSource("safeSamples")
    void ignoresNormalText(RedFlagRule rule, String text) {
        assertThat(rule.findEvidence(contextFor(text))).as(rule.code() + ": " + text).isEmpty();
    }

    @Test
    void freeEmailNeedsHiringContext() {
        FreeEmailRule rule = new FreeEmailRule();
        assertThat(rule.findEvidence(AnalysisContext.of("HR team: tcs.recruit@gmail.com, send your resume")))
                .hasValueSatisfying(e -> assertThat(e).contains("tcs.recruit@gmail.com"));
        assertThat(rule.findEvidence(AnalysisContext.of("Happy birthday! from ravi@gmail.com"))).isEmpty();
        assertThat(rule.findEvidence(AnalysisContext.of("Send your resume to jobs@wipro.com"))).isEmpty();
    }

    @Test
    void domainRulesUseDomainReports() {
        DomainReport lookalike = new DomainReport("infosys-careers.co", null, 20, null, false, false, "infosys.com", false);
        AnalysisContext context = AnalysisContext.of("Apply at infosys-careers.co", Map.of(lookalike.domain(), lookalike));

        assertThat(new LookalikeDomainRule().findEvidence(context)).hasValueSatisfying(e -> assertThat(e).contains("infosys.com"));
        assertThat(new NewDomainRule().findEvidence(context)).hasValueSatisfying(e -> assertThat(e).contains("20 days"));
    }

    @Test
    void oldDomainIsNotNew() {
        DomainReport old = new DomainReport("tcs.com", null, 10_000, null, true, false, null, false);
        AnalysisContext context = AnalysisContext.of("Visit tcs.com", Map.of("tcs.com", old));
        assertThat(new NewDomainRule().findEvidence(context)).isEmpty();
        assertThat(new LookalikeDomainRule().findEvidence(context)).isEmpty();
    }

    private static AnalysisContext contextFor(String text) {
        return AnalysisContext.of(text);
    }
}
