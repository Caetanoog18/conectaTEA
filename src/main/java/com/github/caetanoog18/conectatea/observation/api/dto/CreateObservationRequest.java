package com.github.caetanoog18.conectatea.observation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateObservationRequest(
        @NotBlank
        @Size(max = 120)
        String title,

        @NotBlank
        @Size(max = 5000)
        String content,

        @NotNull
        @PastOrPresent
        Instant occurredAt
) {
}