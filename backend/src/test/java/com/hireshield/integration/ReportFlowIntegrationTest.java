package com.hireshield.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ReportFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void approvedReportFeedsTheBlacklistAndTheScore() throws Exception {
        String user = register("reporter@example.com");
        String admin = login("admin@test.com", "admin-password");

        String report = mvc.perform(post("/api/reports").header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName":"Fake Infotech","upiId":"fakehr@okaxis","platform":"WhatsApp",
                                 "description":"They asked me to pay 2000 rupees as training fee and then blocked me."}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long id = json(report).path("id").asLong();

        // Not public until an admin approves it
        mvc.perform(get("/api/reports/search?q=fakehr")).andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/api/admin/reports").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));

        mvc.perform(patch("/api/admin/reports/" + id).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(get("/api/reports/search?q=fakehr"))
                .andExpect(jsonPath("$[0].value").value("fakehr@okaxis"))
                .andExpect(jsonPath("$[0].type").value("UPI"));

        mvc.perform(post("/api/analyze/text").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Internship offer letter. Send the joining amount to fakehr@okaxis today."}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportedIdentifiers[0]").value("fakehr@okaxis"))
                .andExpect(jsonPath("$.trustScore").value(60));
    }

    @Test
    void reportNeedsAnIdentifier() throws Exception {
        String user = register("noid@example.com");
        mvc.perform(post("/api/reports").header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName":"Someone","description":"This is a long enough description of the scam."}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanTuneRulePoints() throws Exception {
        String admin = login("admin@test.com", "admin-password");
        mvc.perform(put("/api/admin/rules").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"code\":\"POOR_WRITING\",\"points\":8,\"enabled\":false}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'POOR_WRITING')].points").value(8))
                .andExpect(jsonPath("$[?(@.code == 'POOR_WRITING')].enabled").value(false));

        mvc.perform(put("/api/admin/rules").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"code\":\"POOR_WRITING\",\"points\":5,\"enabled\":true}]"))
                .andExpect(status().isOk());
    }

    @Test
    void publicStatsAndModelMetricsAreOpen() throws Exception {
        mvc.perform(get("/api/stats/public")).andExpect(status().isOk()).andExpect(jsonPath("$.totalChecks").exists());
        mvc.perform(get("/api/model/metrics")).andExpect(status().isOk())
                .andExpect(jsonPath("$.test.f1").isNumber())
                .andExpect(jsonPath("$.test.confusionMatrix.truePositive").isNumber());
    }
}
