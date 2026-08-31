package com.github.caetanoog18.conectatea.student.api.dto;

import com.github.caetanoog18.conectatea.student.domain.Student;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        String fullName,
        String preferredName,
        LocalDate birthDate,
        String enrollmentNumber,
        int schoolYear,
        String gradeLevel,
        String className,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getFullName(),
                student.getPreferredName(),
                student.getBirthDate(),
                student.getEnrollmentNumber(),
                student.getSchoolYear(),
                student.getGradeLevel(),
                student.getClassName(),
                student.isActive(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}