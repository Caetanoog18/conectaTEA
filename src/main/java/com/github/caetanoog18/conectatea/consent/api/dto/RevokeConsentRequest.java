package com.github.caetanoog18.conectatea.consent.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeConsentRequest(
        @NotBlank(message = "Revocation reason is required")
        @Size(max = 500, message = "Revocation reason must have at most 500 characters")
        String reason
) {
}