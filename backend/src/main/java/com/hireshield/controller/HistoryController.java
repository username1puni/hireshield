package com.hireshield.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hireshield.dto.AnalysisResponse;
import com.hireshield.dto.AnalysisSummary;
import com.hireshield.dto.FeedbackRequest;
import com.hireshield.dto.PageResponse;
import com.hireshield.entity.RiskLevel;
import com.hireshield.security.AuthUser;
import com.hireshield.service.AnalysisService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
@Tag(name = "History")
public class HistoryController {

    private final AnalysisService analysisService;

    @GetMapping
    public PageResponse<AnalysisSummary> list(@AuthenticationPrincipal AuthUser user,
                                              @RequestParam(required = false) RiskLevel risk,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return analysisService.history(user.getId(), risk, page, size);
    }

    @GetMapping("/{id}")
    public AnalysisResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return analysisService.get(id, user.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        analysisService.delete(id, user.getId());
    }

    @PostMapping("/{id}/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest request,
                         @AuthenticationPrincipal AuthUser user) {
        analysisService.feedback(id, user.getId(), request);
    }
}
