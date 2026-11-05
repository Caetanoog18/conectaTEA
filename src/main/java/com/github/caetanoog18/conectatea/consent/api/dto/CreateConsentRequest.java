package com.github.caetanoog18.conectatea.consent.api.dto;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record CreateConsentRequest(
        @Schema(
                description = "Finalidades autorizadas pelo responsável",
                example = """
                        ["EDUCATIONAL_SUPPORT",
                         "INFORMATION_SHARING_WITH_CARE_TEAM"]
                        """
        )
        @NotEmpty(message = "At least one consent purpose is required")
        Set<ConsentPurpose> purposes,


        @Schema(
                description = "Versão do termo apresentado",
                example = "1.0"
        )
        @NotBlank(message = "Terms version is required")
        @Size(max = 20, message = "Terms version must have at most 20 characters")
        String termsVersion,


        @Schema(
                description = "Momento da concessão em UTC",
                type = "string",
                format = "date-time",
                example = "2026-09-10T12:00:00Z"
        )
        @NotNull(message = "Granted date is required")
        @PastOrPresent(message = "Granted date cannot be in the future")
        Instant grantedAt,


        @Schema(
                description = """
                        Ultimo dia de validade, inclusive.
                        Envie null para um termo sem data final definida.
                        """,
                type = "string",
                format = "date",
                example = "2027-09-10"
        )
        @FutureOrPresent(message = "Valid until date cannot be in the past")
        LocalDate validUntil
) {
}