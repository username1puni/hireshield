package com.hireshield.service;

import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.hireshield.dto.LinkReport;
import com.hireshield.dto.LinkReport.LinkVerdict;
import com.hireshield.entity.LinkCheck;
import com.hireshield.repository.LinkCheckRepository;
import com.hireshield.util.TextPatterns;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

/**
 * Checks a link with simple heuristics, plus Google Safe Browsing when an API key is set.
 */
@Slf4j
@Service
public class LinkService {

    private static final Duration CACHE_TTL = Duration.ofDays(1);
    private static final Set<String> SHORTENERS = Set.of(
            "bit.ly", "tinyurl.com", "t.co", "goo.gl", "rb.gy", "cutt.ly", "is.gd", "shorturl.at", "tiny.cc",
            "ow.ly", "rebrand.ly", "bit.do", "s.id");
    private static final Set<String> RISKY_TLDS = Set.of(
            "xyz", "top", "online", "site", "live", "work", "click", "buzz", "icu", "monster", "rest", "cfd", "sbs", "loan");
    private static final Set<String> CHAT_HOSTS = Set.of("t.me", "telegram.me", "wa.me", "chat.whatsapp.com");
    private static final Pattern IP_HOST = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private final LinkCheckRepository repository;
    private final RestClient safeBrowsing;
    private final String safeBrowsingKey;

    public LinkService(LinkCheckRepository repository, RestClient.Builder builder,
                       @Value("${app.safe-browsing.api-key:}") String safeBrowsingKey) {
        this.repository = repository;
        this.safeBrowsingKey = safeBrowsingKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(4));
        this.safeBrowsing = builder.clone()
                .baseUrl("https://safebrowsing.googleapis.com")
                .requestFactory(factory)
                .build();
    }

    public LinkReport check(String url) {
        String hash = sha256(url);
        var cached = repository.findById(hash)
                .filter(c -> c.getCheckedAt().isAfter(Instant.now().minus(CACHE_TTL)));
        if (cached.isPresent()) {
            LinkCheck c = cached.get();
            List<String> reasons = c.getReasons() == null || c.getReasons().isBlank()
                    ? List.of() : Arrays.asList(c.getReasons().split("\\|"));
            return new LinkReport(c.getUrl(), TextPatterns.domainOf(c.getUrl()), LinkVerdict.valueOf(c.getVerdict()), reasons);
        }
        LinkReport report = evaluate(url);
        LinkCheck entity = new LinkCheck();
        entity.setUrlHash(hash);
        entity.setUrl(url.length() > 2048 ? url.substring(0, 2048) : url);
        entity.setVerdict(report.verdict().name());
        entity.setReasons(String.join("|", report.reasons()));
        entity.setCheckedAt(Instant.now());
        repository.save(entity);
        return report;
    }

    LinkReport evaluate(String url) {
        String host = TextPatterns.domainOf(url);
        List<String> reasons = new ArrayList<>();
        int risk = 0;

        if (SHORTENERS.contains(host)) {
            reasons.add("Shortened link hides the real website");
            risk += 1;
        }
        if (IP_HOST.matcher(host).matches()) {
            reasons.add("Link uses a raw IP address instead of a website name");
            risk += 2;
        }
        if (host.startsWith("xn--") || host.contains(".xn--") || !IDN.toASCII(host).equals(host)) {
            reasons.add("Website name uses look-alike characters (punycode)");
            risk += 2;
        }
        String tld = host.contains(".") ? host.substring(host.lastIndexOf('.') + 1) : "";
        if (RISKY_TLDS.contains(tld)) {
            reasons.add("." + tld + " domains are cheap and often used for scams");
            risk += 1;
        }
        if (CHAT_HOSTS.contains(host)) {
            reasons.add("Link opens a WhatsApp/Telegram chat, not a company website");
            risk += 1;
        }
        if (url.toLowerCase().startsWith("http://")) {
            reasons.add("Not using HTTPS");
            risk += 1;
        }
        String lookalike = DomainService.lookalikeOf(host);
        if (lookalike != null) {
            reasons.add("Looks like " + lookalike + " but is a different site");
            risk += 2;
        }
        if (isListedBySafeBrowsing(url)) {
            reasons.add("Listed by Google Safe Browsing as harmful");
            risk += 5;
        }

        LinkVerdict verdict = risk >= 4 ? LinkVerdict.DANGEROUS : risk >= 1 ? LinkVerdict.SUSPICIOUS : LinkVerdict.SAFE;
        return new LinkReport(url, host, verdict, reasons);
    }

    private boolean isListedBySafeBrowsing(String url) {
        if (safeBrowsingKey == null || safeBrowsingKey.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> body = Map.of(
                    "client", Map.of("clientId", "hireshield", "clientVersion", "1.0"),
                    "threatInfo", Map.of(
                            "threatTypes", List.of("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE"),
                            "platformTypes", List.of("ANY_PLATFORM"),
                            "threatEntryTypes", List.of("URL"),
                            "threatEntries", List.of(Map.of("url", url))));
            JsonNode response = safeBrowsing.post()
                    .uri("/v4/threatMatches:find?key={key}", safeBrowsingKey)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && response.has("matches");
        } catch (RuntimeException e) {
            log.warn("Safe Browsing lookup failed: {}", e.getMessage());
            return false;
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
