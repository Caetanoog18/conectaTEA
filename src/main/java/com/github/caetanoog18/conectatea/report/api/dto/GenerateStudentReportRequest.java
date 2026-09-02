package com.github.caetanoog18.conectatea.report.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record GenerateStudentReportRequest(
        @NotNull(message = "Start date is required")
        Instant from,

        @NotNull(message = "End date is required")
        Instant to
) {
}