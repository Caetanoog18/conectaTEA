package com.github.caetanoog18.conectatea.guardian.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateGuardianStatusRequest(
        @NotNull(message = "Active status is required")
        Boolean active
) {
}
