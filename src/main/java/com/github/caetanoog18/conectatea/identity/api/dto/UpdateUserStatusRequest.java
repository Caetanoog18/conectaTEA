package com.github.caetanoog18.conectatea.identity.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "Active status is required")
        Boolean active
) {
}
