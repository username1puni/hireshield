package com.hireshield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkCheckRequest(@NotBlank @Size(max = 2048) String url) {
}
