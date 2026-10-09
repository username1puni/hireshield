package com.hireshield.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import com.hireshield.dto.DomainReport;
import com.hireshield.entity.DomainCheck;
import com.hireshield.repository.DomainCheckRepository;
import com.hireshield.rules.FreeEmailProviders;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

/**
 * Domain age (RDAP), mail server (MX) and look-alike checks, cached for a week.
 */
@Slf4j
@Service
public class DomainService {

    private static final Duration CACHE_TTL = Duration.ofDays(7);

    // Official domains of companies that scammers like to copy. Key is the brand word.
    private static final Map<String, String> BRANDS = Map.ofEntries(
            Map.entry("tcs", "tcs.com"), Map.entry("infosys", "infosys.com"), Map.entry("wipro", "wipro.com"),
            Map.entry("hcltech", "hcltech.com"), Map.entry("techmahindra", "techmahindra.com"),
            Map.entry("accenture", "accenture.com"), Map.entry("cognizant", "cognizant.com"),
            Map.entry("capgemini", "capgemini.com"), Map.entry("deloitte", "deloitte.com"),
            Map.entry("amazon", "amazon.com"), Map.entry("flipkart", "flipkart.com"),
            Map.entry("google", "google.com"), Map.entry("microsoft", "microsoft.com"),
            Map.entry("ibm", "ibm.com"), Map.entry("oracle", "oracle.com"), Map.entry("ltimindtree", "ltimindtree.com"),
            Map.entry("genpact", "genpact.com"), Map.entry("hdfcbank", "hdfcbank.com"),
            Map.entry("icicibank", "icicibank.com"), Map.entry("airtel", "airtel.in"),
            Map.entry("reliance", "ril.com"), Map.entry("zomato", "zomato.com"), Map.entry("swiggy", "swiggy.com"),
            Map.entry("paytm", "paytm.com"), Map.entry("naukri", "naukri.com"), Map.entry("linkedin", "linkedin.com"),
            Map.entry("indeed", "indeed.com"), Map.entry("internshala", "internshala.com"));

    // Legit secondary domains that contain a brand word.
    private static final List<String> KNOWN_OFFICIAL = List.of(
            "amazon.in", "amazon.jobs", "google.co.in", "tcs.com", "infosys.com", "wipro.com",
            "careers.google.com", "microsoft.com", "linkedin.com", "naukri.com", "indeed.com", "in.indeed.com",
            "internshala.com", "flipkartcareers.com", "accenture.com", "amazonaws.com");

    private final DomainCheckRepository repository;
    private final RestClient rdapClient;

    public DomainService(DomainCheckRepository repository, RestClient.Builder builder) {
        this.repository = repository;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(4));
        this.rdapClient = builder.clone()
                .baseUrl("https://rdap.org")
                .requestFactory(factory)
                .build();
    }

    public DomainReport check(String rawDomain) {
        String domain = rawDomain.toLowerCase(Locale.ROOT).trim();
        Optional<DomainCheck> cached = repository.findById(domain)
                .filter(c -> c.getCheckedAt().isAfter(Instant.now().minus(CACHE_TTL)));
        DomainCheck check = cached.orElseGet(() -> repository.save(lookup(domain)));
        return toReport(check);
    }

    private DomainCheck lookup(String domain) {
        DomainCheck check = new DomainCheck();
        check.setDomain(domain);
        check.setFreeProvider(FreeEmailProviders.isFree(domain));
        check.setLookalikeOf(lookalikeOf(domain));
        check.setCheckedAt(Instant.now());
        if (!check.isFreeProvider()) {
            fillRegistration(check);
            check.setHasMx(hasMailServer(domain));
        }
        return check;
    }

    private void fillRegistration(DomainCheck check) {
        try {
            JsonNode body = rdapClient.get()
                    .uri("/domain/{domain}", registrableDomain(check.getDomain()))
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) {
                return;
            }
            for (JsonNode event : body.path("events")) {
                if ("registration".equals(event.path("eventAction").asString())) {
                    LocalDate registered = OffsetDateTime.parse(event.path("eventDate").asString()).toLocalDate();
                    check.setRegisteredOn(registered);
                    check.setAgeDays((int) ChronoUnit.DAYS.between(registered, LocalDate.now()));
                }
            }
            for (JsonNode entity : body.path("entities")) {
                if (entity.path("roles").toString().contains("registrar")) {
                    JsonNode vcard = entity.path("vcardArray").path(1);
                    for (JsonNode field : vcard) {
                        if ("fn".equals(field.path(0).asString())) {
                            check.setRegistrar(field.path(3).asString());
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            // Not every registry supports RDAP (many ccTLDs do not). Age stays unknown.
            log.debug("RDAP lookup failed for {}: {}", check.getDomain(), e.getMessage());
        }
    }

    private Boolean hasMailServer(String domain) {
        try {
            Lookup lookup = new Lookup(domain, Type.MX);
            Record[] records = lookup.run();
            if (lookup.getResult() == Lookup.HOST_NOT_FOUND || lookup.getResult() == Lookup.TYPE_NOT_FOUND) {
                return false;
            }
            return records != null && records.length > 0 ? Boolean.TRUE : null;
        } catch (Exception e) {
            log.debug("MX lookup failed for {}: {}", domain, e.getMessage());
            return null;
        }
    }

    static String lookalikeOf(String domain) {
        String registrable = registrableDomain(domain);
        if (isOfficial(domain)) {
            return null;
        }
        String label = registrable.substring(0, registrable.indexOf('.') < 0 ? registrable.length() : registrable.indexOf('.'));
        String compact = label.replace("-", "");
        for (Map.Entry<String, String> brand : BRANDS.entrySet()) {
            String name = brand.getKey();
            // infosys-careers.co, tcsjobs.in
            if (name.length() >= 4 && compact.contains(name)) {
                return brand.getValue();
            }
            if (name.length() == 3 && (label.startsWith(name + "-") || label.endsWith("-" + name))) {
                return brand.getValue();
            }
            // infosyss.com, wlpro.com
            if (name.length() >= 5 && levenshtein(compact, name) <= 2 && !compact.equals(name)) {
                return brand.getValue();
            }
        }
        return null;
    }

    private static boolean isOfficial(String domain) {
        for (String official : KNOWN_OFFICIAL) {
            if (domain.equals(official) || domain.endsWith("." + official)) {
                return true;
            }
        }
        return BRANDS.values().stream().anyMatch(d -> domain.equals(d) || domain.endsWith("." + d));
    }

    /** jobs.example.co.in -> example.co.in */
    static String registrableDomain(String domain) {
        String[] parts = domain.split("\\.");
        if (parts.length <= 2) {
            return domain;
        }
        String last2 = parts[parts.length - 2] + "." + parts[parts.length - 1];
        boolean twoLevelSuffix = List.of("co.in", "org.in", "net.in", "gov.in", "nic.in", "ac.in", "edu.in",
                "co.uk", "com.au").contains(last2);
        int keep = twoLevelSuffix ? 3 : 2;
        StringBuilder result = new StringBuilder();
        for (int i = parts.length - keep; i < parts.length; i++) {
            if (!result.isEmpty()) {
                result.append('.');
            }
            result.append(parts[i]);
        }
        return result.toString();
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    private static DomainReport toReport(DomainCheck check) {
        String d = check.getDomain();
        return new DomainReport(d, check.getRegisteredOn(), check.getAgeDays(), check.getRegistrar(),
                check.getHasMx(), check.isFreeProvider(), check.getLookalikeOf(),
                d.endsWith(".gov.in") || d.endsWith(".nic.in") || d.endsWith(".gov"));
    }
}
