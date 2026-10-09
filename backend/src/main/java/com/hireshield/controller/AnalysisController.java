package com.hireshield.controller;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hireshield.dto.AnalysisResponse;
import com.hireshield.dto.TextAnalysisRequest;
import com.hireshield.dto.UrlAnalysisRequest;
import com.hireshield.entity.InputType;
import com.hireshield.exception.BadRequestException;
import com.hireshield.security.AuthUser;
import com.hireshield.service.AnalysisService;
import com.hireshield.service.RateLimiter;
import com.hireshield.service.TextExtractionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
@Tag(name = "Analyze")
public class AnalysisController {

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final int MIN_TEXT = 20;

    private final AnalysisService analysisService;
    private final TextExtractionService extractionService;
    private final RateLimiter rateLimiter;

    @PostMapping("/text")
    @Operation(summary = "Score pasted text. Works without login (rate limited).")
    public AnalysisResponse analyzeText(@Valid @RequestBody TextAnalysisRequest request,
                                        @AuthenticationPrincipal AuthUser user, HttpServletRequest http) {
        rateLimiter.check(user != null ? "user:" + user.getId() : "ip:" + clientIp(http), user != null);
        return analysisService.analyze(request.text().trim(), InputType.TEXT, userId(user), null, null, request.language());
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Score a screenshot (OCR) or a PDF offer letter, max 5 MB")
    public AnalysisResponse analyzeFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "language", required = false) String language,
                                        @AuthenticationPrincipal AuthUser user) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("The file is empty");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException("File is too large. The limit is 5 MB.");
        }
        rateLimiter.check("user:" + user.getId(), true);
        String type = detectType(file);
        String text;
        InputType inputType;
        if (TextExtractionService.PDF_TYPE.equals(type)) {
            text = extractionService.fromPdf(file);
            inputType = InputType.PDF;
        } else {
            text = extractionService.fromImage(file);
            inputType = InputType.IMAGE;
        }
        if (text.length() < MIN_TEXT) {
            throw new BadRequestException("Could not find enough text in this file. Try a clearer screenshot.");
        }
        var info = new AnalysisService.FileInfo(cleanName(file.getOriginalFilename()), type, file.getSize(),
                sha256(file.getBytes()));
        return analysisService.analyze(text, inputType, user.getId(), null, info, language);
    }

    @PostMapping("/url")
    @Operation(summary = "Fetch a job page and score its text")
    public AnalysisResponse analyzeUrl(@Valid @RequestBody UrlAnalysisRequest request,
                                       @AuthenticationPrincipal AuthUser user) {
        rateLimiter.check("user:" + user.getId(), true);
        String text = extractionService.fromUrl(request.url());
        if (text.length() < MIN_TEXT) {
            throw new BadRequestException("That page has almost no text to check");
        }
        // Include the link itself so the domain and link checks run on it.
        String withLink = request.url() + "\n" + text;
        return analysisService.analyze(withLink, InputType.URL, user.getId(), request.url(), null, request.language());
    }

    /** Checks the first bytes instead of trusting the file name or the browser's content type. */
    static String detectType(MultipartFile file) throws IOException {
        byte[] head = new byte[12];
        int read;
        try (var in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        }
        if (read >= 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F') {
            return TextExtractionService.PDF_TYPE;
        }
        if (read >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return "image/png";
        }
        if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "image/webp";
        }
        throw new BadRequestException("Only PNG, JPG, WEBP images and PDF files are supported");
    }

    private static String cleanName(String name) {
        if (name == null || name.isBlank()) {
            return "upload";
        }
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        return base.length() > 200 ? base.substring(0, 200) : base;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Long userId(AuthUser user) {
        return user == null ? null : user.getId();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim().toLowerCase(Locale.ROOT);
        }
        return request.getRemoteAddr();
    }
}
