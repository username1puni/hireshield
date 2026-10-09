package com.hireshield.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(@NotNull Boolean correct, @Size(max = 1000) String comment) {
}
