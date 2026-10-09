package com.hireshield.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hireshield.dto.BlacklistEntryResponse;
import com.hireshield.dto.ScamReportRequest;
import com.hireshield.dto.ScamReportResponse;
import com.hireshield.security.AuthUser;
import com.hireshield.service.BlacklistService;
import com.hireshield.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Community reports")
public class ReportController {

    private final ReportService reportService;
    private final BlacklistService blacklistService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report a scam. It goes live after an admin approves it.")
    public ScamReportResponse create(@Valid @RequestBody ScamReportRequest request,
                                     @AuthenticationPrincipal AuthUser user) {
        return reportService.create(user.getId(), request);
    }

    @GetMapping("/search")
    @Operation(summary = "Search approved scam emails, phones, UPI IDs and websites")
    public List<BlacklistEntryResponse> search(@RequestParam("q") String query) {
        return blacklistService.search(query);
    }
}
