package com.github.caetanoog18.conectatea.student.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must contain at most 120 characters")
        String fullName,

        @Size(max = 120, message = "Preferred name must contain at most 120 characters")
        String preferredName,

        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @NotBlank(message = "Enrollment number is required")
        @Size(max = 50, message = "Enrollment number must contain at most 50 characters")
        String enrollmentNumber,

        @NotNull(message = "School year is required")
        @Min(value = 2000, message = "School year must be at least 2000")
        @Max(value = 2100, message = "School year must be at most 2100")
        Integer schoolYear,

        @NotBlank(message = "Grade level is required")
        @Size(max = 50, message = "Grade level must contain at most 50 characters")
        String gradeLevel,

        @Size(max = 30, message = "Class name must contain at most 30 characters")
        String className
) {
}