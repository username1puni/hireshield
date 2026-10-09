package com.hireshield.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ScamReportRequest(
        @Size(max = 200) String companyName,
        @Email @Size(max = 255) String email,
        @Pattern(regexp = "^[+0-9 -]{8,20}$", message = "must be a valid phone number") String phone,
        @Pattern(regexp = "^[A-Za-z0-9._-]{2,}@[A-Za-z]{2,}$", message = "must look like name@bank") String upiId,
        @Size(max = 255) String domain,
        @Size(max = 50) String platform,
        @PositiveOrZero BigDecimal amountLost,
        @NotBlank @Size(min = 20, max = 5000) String description) {

    @AssertTrue(message = "Add at least one of email, phone, UPI ID or website")
    public boolean isIdentifierPresent() {
        return notBlank(email) || notBlank(phone) || notBlank(upiId) || notBlank(domain);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
