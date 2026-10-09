package com.hireshield.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hireshield.dto.BlacklistEntryResponse;
import com.hireshield.entity.BlacklistEntry;
import com.hireshield.entity.BlacklistType;
import com.hireshield.entity.ScamReport;
import com.hireshield.repository.BlacklistEntryRepository;
import com.hireshield.rules.AnalysisContext;
import com.hireshield.util.TextPatterns;

import lombok.RequiredArgsConstructor;

/**
 * Identifiers (email, phone, UPI, website) confirmed as scams by an admin.
 */
@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final BlacklistEntryRepository repository;

    /** Identifiers in the message that other users already reported. */
    @Transactional(readOnly = true)
    public List<String> findReported(AnalysisContext context) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.addAll(context.emails());
        candidates.addAll(context.upiIds());
        context.phones().forEach(p -> candidates.add(normalizePhone(p)));
        context.domains().keySet().forEach(candidates::add);
        if (candidates.isEmpty()) {
            return List.of();
        }
        return repository.findByValueIn(candidates).stream().map(BlacklistEntry::getValue).distinct().toList();
    }

    @Transactional
    public void addFromReport(ScamReport report) {
        add(BlacklistType.EMAIL, report.getEmail());
        add(BlacklistType.PHONE, report.getPhone() == null ? null : normalizePhone(report.getPhone()));
        add(BlacklistType.UPI, report.getUpiId());
        add(BlacklistType.DOMAIN, report.getDomain() == null ? null : TextPatterns.domainOf(report.getDomain()));
    }

    @Transactional(readOnly = true)
    public List<BlacklistEntryResponse> search(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 3) {
            return List.of();
        }
        List<BlacklistEntryResponse> result = new ArrayList<>();
        repository.search(q, PageRequest.of(0, 20)).forEach(e -> result.add(BlacklistEntryResponse.from(e)));
        return result;
    }

    private void add(BlacklistType type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return;
        }
        String value = rawValue.trim().toLowerCase(Locale.ROOT);
        repository.findByTypeAndValue(type, value).ifPresentOrElse(entry -> {
            entry.setReportCount(entry.getReportCount() + 1);
            entry.setLastSeen(Instant.now());
        }, () -> repository.save(new BlacklistEntry(type, value)));
    }

    /** Keep the last 10 digits so +91 98765 43210 and 9876543210 match. */
    static String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }
}
