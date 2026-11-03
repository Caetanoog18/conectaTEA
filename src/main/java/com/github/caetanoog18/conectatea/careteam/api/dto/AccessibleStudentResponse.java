package com.github.caetanoog18.conectatea.careteam.api.dto;

import com.github.caetanoog18.conectatea.student.domain.Student;

import java.util.UUID;

public record AccessibleStudentResponse(
        UUID id,
        String fullName,
        String preferredName,
        String enrollmentNumber,
        int schoolYear,
        String gradeLevel,
        String className
) {
    public static AccessibleStudentResponse from(Student student) {
        return new AccessibleStudentResponse(
                student.getId(),
                student.getFullName(),
                student.getPreferredName(),
                student.getEnrollmentNumber(),
                student.getSchoolYear(),
                student.getGradeLevel(),
                student.getClassName()
        );
    }
}