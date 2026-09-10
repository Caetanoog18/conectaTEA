package com.github.caetanoog18.conectatea.consent.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record RevokeConsentRequest(
        @Schema(
                description = "Motivo da revogação",
                example = "O responsável retirou a autorização"
        )
        @NotBlank(message = "Revocation reason is required")
        @Size(max = 500, message = "Revocation reason must have at most 500 characters")
        String reason
) {
}