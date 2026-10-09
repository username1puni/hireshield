package com.hireshield.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hireshield.dto.PublicStatsResponse;
import com.hireshield.service.ModelService;
import com.hireshield.service.StatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Stats")
public class StatsController {

    private final StatsService statsService;
    private final ModelService modelService;

    @GetMapping("/stats/public")
    @Operation(summary = "Numbers for the home page")
    public PublicStatsResponse publicStats() {
        return statsService.publicStats();
    }

    @GetMapping("/model/metrics")
    @Operation(summary = "Test-set metrics of the model in use")
    public JsonNode modelMetrics() {
        return modelService.metrics();
    }
}
