package com.github.caetanoog18.conectatea.report.api.dto;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.observation.api.dto.ObservationResponse;
import com.github.caetanoog18.conectatea.student.domain.Student;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentReportResponse(
        UUID reportId,
        Instant generatedAt,
        StudentSummary student,
        RequesterSummary generatedBy,
        ConsentPurpose purpose,
        Instant from,
        Instant to,
        int totalObservations,
        List<ObservationResponse> observations
) {

    public StudentReportResponse {
        observations = List.copyOf(observations);
    }

    public record StudentSummary(
            UUID id,
            String fullName,
            String preferredName,
            String enrollmentNumber,
            int schoolYear,
            String gradeLevel,
            String className
    ) {

        public static StudentSummary from(Student student) {
            return new StudentSummary(
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

    public record RequesterSummary(UUID id, String fullName) {
        public static RequesterSummary from(User user) {
            return new RequesterSummary(user.getId(), user.getFullName());
        }
    }
}