package com.github.caetanoog18.conectatea.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais para autenticação")
public record LoginRequest(
        @Schema(
                description = "E-mail da conta cadastrada",
                example = "admin@conectatea.com",
                format = "email"
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(
                description = "Senha atual da conta. Não informe o hash.",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Password is required")
        String password
) {
}