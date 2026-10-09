package com.hireshield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UrlAnalysisRequest(
        @NotBlank @Size(max = 2048) @Pattern(regexp = "(?i)https?://.+", message = "must start with http:// or https://") String url,
        @Pattern(regexp = "en|hi", message = "must be en or hi") String language) {
}
