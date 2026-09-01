package com.github.caetanoog18.conectatea.guardian.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.util.UUID;

public record GuardianRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must contain at most 120 characteres")
        String fullName,

        @CPF(message = "CPF must be valid")
        String cpf,

        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must contain at most 254 characters")
        String email,

        @NotBlank(message = "Phone is required")
        @Size(max = 20, message = "Phone must contain at most 20 characters")
        String phone,

        UUID userId
) {
}
