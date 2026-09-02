package com.github.caetanoog18.conectatea.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acesso emitido depois da autenticação")
public record TokenResponse(
        @Schema(
                description = "JWT utilizado no cabeçalho Authorization. " +
                        "Não compartilhe nem registre este valor em logs."
        )
        String accessToken,

        @Schema(description = "Tipo de autenticação", example = "Bearer")
        String tokenType,

        @Schema(description = "Tempo de validade do token, em segundos", example = "3600")
        long expiresIn
) {
}