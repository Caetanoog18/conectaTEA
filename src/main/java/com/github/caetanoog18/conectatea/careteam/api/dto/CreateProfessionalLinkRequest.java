package com.github.caetanoog18.conectatea.careteam.api.dto;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

public record CreateProfessionalLinkRequest(
        @Schema(
                description = "Identificador do usuário profissional",
                format = "uuid"
        )
        @NotNull(message = "Professional ID is required")
        UUID professionalId,

        @Schema(
                description = """
                        Primeiro dia do vínculo. Não pode estar no futuro.
                        A data é interpretada em UTC.
                        """,
                type = "string",
                format = "date",
                example = "2026-09-10"
        )
        @NotNull(message = "Start date is required")
        LocalDate startedOn
) {
}