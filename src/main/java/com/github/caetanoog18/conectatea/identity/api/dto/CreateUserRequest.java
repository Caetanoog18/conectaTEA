package com.github.caetanoog18.conectatea.identity.api.dto;

import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;


public record CreateUserRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must contain at most 120 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must contain at most 254 characters")
        String email,

        @Schema(
                description = "Senha inicial, entre 12 e 64 caracteres",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Password is required")
        @Size(
                min = 12,
                max = 64,
                message = "Password must contain between 12 and 64 characters"
        )
        String password,

        @NotNull(message = "Role is required")
        UserRole role
) {
}
