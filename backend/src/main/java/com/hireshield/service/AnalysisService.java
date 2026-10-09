package com.hireshield.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hireshield.dto.AnalysisResponse;
import com.hireshield.dto.AnalysisSummary;
import com.hireshield.dto.DomainReport;
import com.hireshield.dto.FeedbackRequest;
import com.hireshield.dto.FlagResponse;
import com.hireshield.dto.LinkReport;
import com.hireshield.dto.PageResponse;
import com.hireshield.entity.Analysis;
import com.hireshield.entity.AnalysisFlag;
import com.hireshield.entity.Feedback;
import com.hireshield.entity.InputType;
import com.hireshield.entity.RiskLevel;
import com.hireshield.entity.UploadedFile;
import com.hireshield.exception.NotFoundException;
import com.hireshield.ml.ScamClassifier;
import com.hireshield.repository.AnalysisRepository;
import com.hireshield.repository.FeedbackRepository;
import com.hireshield.repository.ModelVersionRepository;
import com.hireshield.repository.UploadedFileRepository;
import com.hireshield.repository.UserRepository;
import com.hireshield.rules.AnalysisContext;
import com.hireshield.rules.RuleEngine;
import com.hireshield.rules.RuleHit;
import com.hireshield.util.TextPatterns;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

/**
 * The main flow: extract details, run rules + ML + trust checks, combine, explain, save.
 */
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final int MAX_DOMAINS = 4;
    private static final int MAX_LINKS = 5;

    private final RuleEngine ruleEngine;
    private final ModelService modelService;
    private final DomainService domainService;
    private final LinkService linkService;
    private final BlacklistService blacklistService;
    private final ScoringService scoringService;
    private final ExplanationService explanationService;
    private final AnalysisRepository analysisRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ModelVersionRepository modelVersionRepository;

    // Domain and link lookups are network calls, so run them side by side.
    private final ExecutorService lookups = Executors.newVirtualThreadPerTaskExecutor();

    public record FileInfo(String fileName, String mimeType, long size, String sha256) {
    }

    @Transactional
    public AnalysisResponse analyze(String text, InputType inputType, Long userId, String sourceUrl,
                                    FileInfo file, String language) {
        Result result = run(text, language);

        Analysis analysis = new Analysis();
        if (userId != null) {
            analysis.setUser(userRepository.getReferenceById(userId));
        }
        analysis.setInputType(inputType);
        analysis.setRawText(text);
        analysis.setSourceUrl(sourceUrl);
        analysis.setRecruiterEmail(result.context().emails().isEmpty() ? null : result.context().emails().get(0));
        analysis.setDomain(result.domains().isEmpty() ? null : result.domains().get(0).domain());
        analysis.setFinalScore(result.score().finalScore());
        analysis.setRiskLevel(result.score().riskLevel());
        analysis.setRuleScore(result.score().ruleScore());
        analysis.setMlProbability(BigDecimal.valueOf(result.ml().probability()).setScale(4, RoundingMode.HALF_UP));
        analysis.setTrustScore(result.score().trustScore());
        analysis.setExplanation(result.explanation().text());
        analysis.setExplanationSource(result.explanation().source());
        if (modelService.activeVersionId() != null) {
            analysis.setModelVersion(modelVersionRepository.getReferenceById(modelService.activeVersionId()));
        }
        result.hits().forEach(h -> analysis.addFlag(new AnalysisFlag(h.code(), h.title(), h.points(), truncate(h.evidence()))));
        analysisRepository.save(analysis);

        if (file != null) {
            UploadedFile uploaded = new UploadedFile();
            uploaded.setAnalysis(analysis);
            uploaded.setFileName(file.fileName());
            uploaded.setMimeType(file.mimeType());
            uploaded.setSizeBytes(file.size());
            uploaded.setSha256(file.sha256());
            uploadedFileRepository.save(uploaded);
        }
        return toResponse(analysis, result);
    }

    @Transactional(readOnly = true)
    public PageResponse<AnalysisSummary> history(Long userId, RiskLevel risk, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<Analysis> result = risk == null
                ? analysisRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : analysisRepository.findByUserIdAndRiskLevelOrderByCreatedAtDesc(userId, risk, pageable);
        return PageResponse.from(result.map(AnalysisSummary::from));
    }

    @Transactional(readOnly = true)
    public AnalysisResponse get(Long id, Long userId) {
        Analysis analysis = findOwned(id, userId);
        // Domain and link results come from their cache tables, the ML part is recomputed.
        Result details = details(analysis.getRawText());
        List<RuleHit> storedHits = analysis.getFlags().stream()
                .map(f -> new RuleHit(f.getRuleCode(), f.getTitle(), f.getPoints(), f.getEvidence()))
                .toList();
        Result stored = new Result(details.context(), storedHits, details.ml(), details.domains(), details.links(),
                details.reported(), null, null);
        return toResponse(analysis, stored);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        analysisRepository.delete(findOwned(id, userId));
    }

    @Transactional
    public void feedback(Long id, Long userId, FeedbackRequest request) {
        Analysis analysis = findOwned(id, userId);
        Feedback feedback = feedbackRepository.findByAnalysisIdAndUserId(id, userId).orElseGet(Feedback::new);
        feedback.setAnalysis(analysis);
        feedback.setUser(userRepository.getReferenceById(userId));
        feedback.setCorrect(request.correct());
        feedback.setComment(request.comment());
        feedbackRepository.save(feedback);
    }

    private Analysis findOwned(Long id, Long userId) {
        return analysisRepository.findWithFlagsByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Check " + id + " not found"));
    }

    private record Result(AnalysisContext context, List<RuleHit> hits, ScamClassifier.MlResult ml,
                          List<DomainReport> domains, List<LinkReport> links, List<String> reported,
                          ScoringService.Score score, ExplanationService.Explanation explanation) {
    }

    private Result details(String text) {
        AnalysisContext plain = AnalysisContext.of(text);
        Set<String> domainNames = new LinkedHashSet<>();
        plain.emails().forEach(e -> domainNames.add(TextPatterns.domainOf(e)));
        plain.urls().forEach(u -> domainNames.add(TextPatterns.domainOf(u)));

        List<CompletableFuture<DomainReport>> domainJobs = domainNames.stream().limit(MAX_DOMAINS)
                .map(d -> CompletableFuture.supplyAsync(() -> domainService.check(d), lookups))
                .toList();
        List<CompletableFuture<LinkReport>> linkJobs = plain.urls().stream().limit(MAX_LINKS)
                .map(u -> CompletableFuture.supplyAsync(() -> linkService.check(u), lookups))
                .toList();

        Map<String, DomainReport> domains = new LinkedHashMap<>();
        domainJobs.stream().map(CompletableFuture::join).forEach(r -> domains.put(r.domain(), r));
        List<LinkReport> links = linkJobs.stream().map(CompletableFuture::join).toList();

        AnalysisContext context = AnalysisContext.of(text, domains);
        List<String> reported = blacklistService.findReported(context);
        return new Result(context, List.of(), modelService.predict(text), new ArrayList<>(domains.values()), links,
                reported, null, null);
    }

    private Result run(String text, String language) {
        Result base = details(text);
        List<RuleHit> hits = ruleEngine.evaluate(base.context());
        int ruleScore = RuleEngine.score(hits);
        int trust = scoringService.trustScore(base.domains(), base.links(), base.reported().size());
        ScoringService.Score score = scoringService.combine(ruleScore, base.ml().probability(), trust);
        var explanation = explanationService.explain(score.riskLevel(), score.finalScore(), hits,
                base.ml().topTerms(), trustNotes(base), language);
        return new Result(base.context(), hits, base.ml(), base.domains(), base.links(), base.reported(), score,
                explanation);
    }

    private static List<String> trustNotes(Result r) {
        List<String> notes = new ArrayList<>();
        for (String id : r.reported()) {
            notes.add(id + " was already reported as a scam by other users");
        }
        for (DomainReport d : r.domains()) {
            if (d.lookalikeOf() != null) {
                notes.add(d.domain() + " is pretending to be " + d.lookalikeOf());
            }
            if (!d.freeEmailProvider() && d.isNew(180)) {
                notes.add(d.domain() + " was created only " + d.ageDays() + " days ago");
            }
            if (!d.freeEmailProvider() && Boolean.FALSE.equals(d.hasMx())) {
                notes.add(d.domain() + " cannot receive email (no mail server)");
            }
        }
        for (LinkReport l : r.links()) {
            if (l.verdict() == LinkReport.LinkVerdict.DANGEROUS) {
                notes.add("The link " + l.domain() + " looks dangerous");
            }
        }
        return notes;
    }

    private AnalysisResponse toResponse(Analysis a, Result r) {
        List<FlagResponse> flags = r.hits().stream()
                .map(h -> new FlagResponse(h.code(), h.title(), h.points(), h.evidence()))
                .toList();
        List<String> terms = r.ml().topTerms().stream().map(ScamClassifier.TermContribution::term).toList();
        String version = a.getModelVersion() == null ? null : a.getModelVersion().getVersion();
        return new AnalysisResponse(a.getId(), a.getFinalScore(), a.getRiskLevel(), a.getRuleScore(),
                a.getMlProbability().doubleValue(), a.getTrustScore(), flags, terms, r.domains(), r.links(),
                r.reported(), a.getExplanation(), a.getExplanationSource(), a.getInputType(), a.getRawText(),
                version, a.getCreatedAt());
    }

    private static String truncate(String s) {
        return s == null || s.length() <= 500 ? s : s.substring(0, 497) + "...";
    }

    @PreDestroy
    void shutdown() {
        lookups.shutdown();
    }
}
