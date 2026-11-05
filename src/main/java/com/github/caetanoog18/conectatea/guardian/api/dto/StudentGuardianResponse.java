package com.github.caetanoog18.conectatea.guardian.api.dto;

import com.github.caetanoog18.conectatea.guardian.domain.GuardianRelationship;
import com.github.caetanoog18.conectatea.guardian.domain.StudentGuardian;

import java.time.Instant;
import java.util.UUID;

public record StudentGuardianResponse(
        UUID id,
        UUID studentId,
        String studentName,
        UUID guardianId,
        String guardianName,
        GuardianRelationship relationship,
        boolean legalGuardian,
        boolean primaryContact,
        Instant createdAt,
        Instant updatedAt
) {
    public static StudentGuardianResponse from(StudentGuardian link) {
        return new StudentGuardianResponse(
                link.getId(),
                link.getStudent().getId(),
                link.getStudent().getFullName(),
                link.getGuardian().getId(),
                link.getGuardian().getFullName(),
                link.getRelationship(),
                link.isLegalGuardian(),
                link.isPrimaryContact(),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }
}