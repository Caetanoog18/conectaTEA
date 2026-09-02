package com.github.caetanoog18.conectatea.consent.api.dto;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record CreateConsentRequest(
        @NotEmpty(message = "At least one consent purpose is required")
        Set<ConsentPurpose> purposes,

        @NotBlank(message = "Terms version is required")
        @Size(max = 20, message = "Terms version must have at most 20 characters")
        String termsVersion,

        @NotNull(message = "Granted date is required")
        @PastOrPresent(message = "Granted date cannot be in the future")
        Instant grantedAt,

        @FutureOrPresent(message = "Valid until date cannot be in the past")
        LocalDate validUntil
) {
}