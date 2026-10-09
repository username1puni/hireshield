package com.hireshield.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import tools.jackson.databind.JsonNode;

class AnalysisIntegrationTest extends IntegrationTestBase {

    private static final String SCAM = """
            Congratulations! You are selected for a work from home data entry job. Earn Rs 4000 per day.
            No interview required. Pay Rs 1999 registration fee to confirm your seat. Limited seats, join today.
            Contact HR on WhatsApp 9876543210 or mail hr.desk.jobs@gmail.com""";

    private static final String REAL = """
            We are hiring Java developers for our Pune office. Selection process: online coding test followed by
            technical and HR interviews. Apply through our careers page. We never charge any fee for recruitment.""";

    @Test
    void guestCanCheckTextAndScamScoresHigh() throws Exception {
        mvc.perform(post("/api/analyze/text").contentType(MediaType.APPLICATION_JSON).content(body(SCAM)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.flags[?(@.code == 'FEE_REQUEST')]").exists())
                .andExpect(jsonPath("$.flags[?(@.code == 'FREE_EMAIL')]").exists())
                .andExpect(jsonPath("$.explanationSource").value("TEMPLATE"));
    }

    @Test
    void normalJobPostScoresLow() throws Exception {
        String response = mvc.perform(post("/api/analyze/text").contentType(MediaType.APPLICATION_JSON).content(body(REAL)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode result = json(response);
        assertThat(result.path("riskLevel").asString()).isEqualTo("LOW");
        assertThat(result.path("flags").size()).isZero();
    }

    @Test
    void tooShortTextIsRejected() throws Exception {
        mvc.perform(post("/api/analyze/text").contentType(MediaType.APPLICATION_JSON).content(body("hi")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.text").exists());
    }

    @Test
    void historyFeedbackAndDelete() throws Exception {
        String token = register("history@example.com");
        String auth = bearer(token);

        String created = mvc.perform(post("/api/analyze/text").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(SCAM)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = json(created).path("id").asLong();

        mvc.perform(get("/api/analyses").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].riskLevel").value("HIGH"));

        mvc.perform(get("/api/analyses?risk=LOW").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/analyses/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags").isNotEmpty());

        mvc.perform(post("/api/analyses/" + id + "/feedback").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"correct\":true}"))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/analyses/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/analyses/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotSeeEachOthersChecks() throws Exception {
        String owner = register("owner@example.com");
        String other = register("other@example.com");
        String created = mvc.perform(post("/api/analyze/text").header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body(SCAM)))
                .andReturn().getResponse().getContentAsString();
        long id = json(created).path("id").asLong();

        mvc.perform(get("/api/analyses/" + id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void pdfUploadIsAnalysed() throws Exception {
        String token = register("pdf@example.com");
        MockMultipartFile pdf = new MockMultipartFile("file", "offer.pdf", "application/pdf", SamplePdf.withText(SCAM));

        mvc.perform(multipart("/api/analyze/file").file(pdf).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputType").value("PDF"))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"));
    }

    @Test
    void fakeImageIsRejected() throws Exception {
        String token = register("upload@example.com");
        MockMultipartFile notImage = new MockMultipartFile("file", "photo.png", "image/png", "just text".getBytes());

        mvc.perform(multipart("/api/analyze/file").file(notImage).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadNeedsLogin() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("file", "offer.pdf", "application/pdf", SamplePdf.withText(SCAM));
        mvc.perform(multipart("/api/analyze/file").file(pdf)).andExpect(status().isUnauthorized());
    }

    private String body(String text) {
        return objectMapper.writeValueAsString(java.util.Map.of("text", text));
    }
}
