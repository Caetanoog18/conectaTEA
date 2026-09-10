package com.github.caetanoog18.conectatea.guardian.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record GuardianRequest(
        @Schema(
                description = "Nome completo do responsável",
                example = "Maria da Silva"
        )
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must contain at most 120 characteres")
        String fullName,

        @Schema(
                description = """
                        CPF opcional. Quando informado, deve ser válido.
                        Pode ser enviado com ou sem pontuação.
                        """,
                example = "52998224725"
        )
        @CPF(message = "CPF must be valid")
        String cpf,

        @Schema(
                description = "Email opcional do responsável",
                format = "email",
                example = "maria@example.com"
        )
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must contain at most 254 characters")
        String email,


        @Schema(
                description = "Telefone para contato",
                example = "47999999999"
        )
        @NotBlank(message = "Phone is required")
        @Size(max = 20, message = "Phone must contain at most 20 characters")
        String phone,

        @Schema(
                description = """
                        Usuário LEGAL_GUARDIAN opcional associado ao responsável.
                        Envie null enquanto ele não possuir acesso ao sistema.
                        """,
                format = "uuid"
        )
        UUID userId
) {
}
