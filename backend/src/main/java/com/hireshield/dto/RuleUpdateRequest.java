package com.hireshield.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RuleUpdateRequest(@NotBlank String code, @Min(0) @Max(100) int points, boolean enabled) {
}
