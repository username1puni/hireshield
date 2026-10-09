package com.hireshield.integration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hireshield.dto.DomainReport;
import com.hireshield.dto.LinkReport;
import com.hireshield.service.DomainService;
import com.hireshield.service.LinkService;
import com.hireshield.util.TextPatterns;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Full Spring context on H2. Network lookups (RDAP, DNS, Safe Browsing) are mocked
 * so the tests are fast and do not depend on the internet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected DomainService domainService;

    @MockitoBean
    protected LinkService linkService;

    @BeforeEach
    void stubLookups() {
        when(domainService.check(anyString())).thenAnswer(inv -> {
            String domain = inv.getArgument(0);
            boolean free = domain.equals("gmail.com");
            return new DomainReport(domain, null, free ? null : 5000, null, true, free, null, false);
        });
        when(linkService.check(anyString())).thenAnswer(inv -> {
            String url = inv.getArgument(0);
            return new LinkReport(url, TextPatterns.domainOf(url), LinkReport.LinkVerdict.SAFE, List.of());
        });
    }

    protected String register(String email) throws Exception {
        String body = """
                {"name":"Test User","email":"%s","password":"password123"}""".formatted(email);
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json(response).path("token").asString();
    }

    protected String login(String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}""".formatted(email, password);
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json(response).path("token").asString();
    }

    protected JsonNode json(String body) {
        return objectMapper.readTree(body);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
