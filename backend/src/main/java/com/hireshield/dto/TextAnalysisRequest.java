package com.hireshield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TextAnalysisRequest(
        @NotBlank(message = "Paste the job post or message first")
        @Size(min = 20, max = 20000, message = "must be between 20 and 20000 characters")
        String text,
        @Pattern(regexp = "en|hi", message = "must be en or hi") String language) {
}
