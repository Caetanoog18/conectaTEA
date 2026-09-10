package com.github.caetanoog18.conectatea.careteam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record EndProfessionalLinkRequest(
        @Schema(
                description = "Motivo do encerramento do vínculo",
                example = "O profissional não integra mais a equipe de cuidado"
        )
        @NotBlank(message = "End reason is required")
        @Size(max = 500, message = "End reason must have at most 500 characters")
        String reason
) {
}