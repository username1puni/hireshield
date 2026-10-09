package com.hireshield.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hireshield.dto.PageResponse;
import com.hireshield.dto.ReportReviewRequest;
import com.hireshield.dto.RuleResponse;
import com.hireshield.dto.RuleUpdateRequest;
import com.hireshield.dto.ScamReportResponse;
import com.hireshield.entity.ReportStatus;
import com.hireshield.service.ModelService;
import com.hireshield.service.ReportService;
import com.hireshield.service.RuleAdminService;
import com.hireshield.service.StatsService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminController {

    private final ReportService reportService;
    private final RuleAdminService ruleAdminService;
    private final StatsService statsService;
    private final ModelService modelService;

    @GetMapping("/reports")
    public PageResponse<ScamReportResponse> reports(@RequestParam(defaultValue = "PENDING") ReportStatus status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return reportService.byStatus(status, page, size);
    }

    @PatchMapping("/reports/{id}")
    public ScamReportResponse review(@PathVariable Long id, @Valid @RequestBody ReportReviewRequest request) {
        return reportService.review(id, request);
    }

    @GetMapping("/rules")
    public List<RuleResponse> rules() {
        return ruleAdminService.list();
    }

    @PutMapping("/rules")
    public List<RuleResponse> updateRules(@Valid @RequestBody List<@Valid RuleUpdateRequest> updates) {
        return ruleAdminService.update(updates);
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", modelService.metrics());
        result.put("usage", statsService.usage());
        return result;
    }
}
