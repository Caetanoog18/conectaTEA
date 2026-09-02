package com.github.caetanoog18.conectatea.report.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(
        description = """
                Período de ocorrência das observações.
                O início é incluído e o fim é excluído.
                O início deve ser anterior ao fim.
                O intervalo máximo permitido é de 366 dias.
                """
)
public record GenerateStudentReportRequest(
        @Schema(
                description = "Início incluído, em ISO 8601 com fuso horário",
                type = "string",
                format = "date-time",
                example = "2026-09-01T00:00:00Z"
        )
        @NotNull(message = "Start date is required")
        Instant from,

        @Schema(
                description = "Fim excluído, em ISO 8601 com fuso horário",
                type = "string",
                format = "date-time",
                example = "2026-10-01T00:00:00Z"
        )
        @NotNull(message = "End date is required")
        Instant to
) {
}