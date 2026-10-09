package com.hireshield.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hireshield.dto.DomainReport;
import com.hireshield.dto.LinkCheckRequest;
import com.hireshield.dto.LinkReport;
import com.hireshield.exception.BadRequestException;
import com.hireshield.service.DomainService;
import com.hireshield.service.LinkService;
import com.hireshield.util.TextPatterns;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/check")
@RequiredArgsConstructor
@Validated
@Tag(name = "Quick checks")
public class CheckController {

    private final DomainService domainService;
    private final LinkService linkService;

    @GetMapping("/domain")
    @Operation(summary = "Domain age, mail server and look-alike check")
    public DomainReport domain(@RequestParam("name") @NotBlank
                               @Pattern(regexp = "^[A-Za-z0-9.-]{3,253}$", message = "must be a domain like example.com")
                               String name) {
        String domain = TextPatterns.domainOf(name);
        if (!domain.contains(".")) {
            throw new BadRequestException("must be a domain like example.com");
        }
        return domainService.check(domain);
    }

    @GetMapping("/email")
    @Operation(summary = "Is this recruiter email from a free provider or a risky domain?")
    public Map<String, Object> email(@RequestParam("address") @NotBlank @Email String address) {
        DomainReport report = domainService.check(TextPatterns.domainOf(address));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("address", address.toLowerCase());
        result.put("freeEmailProvider", report.freeEmailProvider());
        result.put("domain", report);
        result.put("advice", report.freeEmailProvider()
                ? "Company recruiters normally write from the company's own domain, not a free email service."
                : "The email uses its own domain. Check that it matches the company's official website.");
        return result;
    }

    @PostMapping("/link")
    @Operation(summary = "Check a link for common scam signs")
    public LinkReport link(@Valid @RequestBody LinkCheckRequest request) {
        return linkService.check(request.url().trim());
    }
}
