package com.github.caetanoog18.conectatea.careteam.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateProfessionalLinkRequest(
        @NotNull(message = "Professional ID is required")
        UUID professionalId,

        @NotNull(message = "Start date is required")
        LocalDate startedOn
) {
}