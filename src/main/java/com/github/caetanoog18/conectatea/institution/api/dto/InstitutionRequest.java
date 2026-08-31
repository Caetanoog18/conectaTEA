package com.github.caetanoog18.conectatea.institution.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InstitutionRequest(
        @NotBlank(message = "Institution name is required")
        @Size(max = 160, message = "Institution name must contain at most 160 characters")
        String name,

        @Pattern(
                regexp = "^\\d{14}$|^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$",
                message = "Tax ID must be a valid CNPJ format"
        )
        String taxId,

        @NotBlank(message = "Institution email is required")
        @Email(message = "Institution email must be valid")
        @Size(max = 254, message = "Institution email must contain at most 254 characters")
        String email,

        @Size(max = 20, message = "Phone must contain at most 20 characters")
        String phone,

        @Size(max = 160, message = "Street must contain at most 160 characters")
        String street,

        @Size(max = 20, message = "Address number must contain at most 20 characters")
        String addressNumber,

        @Size(max = 100, message = "Complement must contain at most 100 characters")
        String complement,

        @Size(max = 100, message = "District must contain at most 100 characters")
        String district,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must contain at most 100 characters")
        String city,

        @NotBlank(message = "State is required")
        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "State must contain exactly two letters"
        )
        String state,

        @Pattern(
                regexp = "^\\d{8}$|^\\d{5}-\\d{3}$",
                message = "Postal code must be a valid CEP format"
        )
        String postalCode
) {
}