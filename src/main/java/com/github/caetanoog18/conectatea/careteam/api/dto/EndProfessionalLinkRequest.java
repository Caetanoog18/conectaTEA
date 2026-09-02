package com.github.caetanoog18.conectatea.careteam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EndProfessionalLinkRequest(
        @NotBlank(message = "End reason is required")
        @Size(max = 500, message = "End reason must have at most 500 characters")
        String reason
) {
}