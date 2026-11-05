package com.github.caetanoog18.conectatea.student.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStudentStatusRequest(
        @NotNull(message = "Active status is required")
        Boolean active
) {
}