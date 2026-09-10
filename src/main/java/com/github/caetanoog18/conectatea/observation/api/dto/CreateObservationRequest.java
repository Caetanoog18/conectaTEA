package com.github.caetanoog18.conectatea.observation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record CreateObservationRequest(
        @Schema(
                description = "Título curto da observação",
                example = "Participação em atividade"
        )
        @NotBlank
        @Size(max = 120)
        String title,

        @Schema(
                description = """
                        Descrição profissional da situação observada.
                        Não inclua senhas ou credenciais.
                        """,
                example = "Participou da atividade com apoio visual."
        )
        @NotBlank
        @Size(max = 5000)
        String content,

        @Schema(
                description = """
                        Momento em que a situação ocorreu.
                        Não pode estar no futuro.
                        """,
                type = "string",
                format = "date-time",
                example = "2026-09-10T13:30:00Z"
        )
        @NotNull
        @PastOrPresent
        Instant occurredAt
) {
}