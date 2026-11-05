package com.github.caetanoog18.conectatea.careteam.api.dto;

import com.github.caetanoog18.conectatea.careteam.domain.StudentProfessionalLink;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProfessionalLinkResponse(
        UUID id,
        UUID studentId,
        String studentName,
        UUID professionalId,
        String professionalName,
        UserRole professionalRole,
        LocalDate startedOn,
        LocalDate endedOn,
        boolean active,
        UUID createdByUserId,
        UUID endedByUserId,
        String endReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProfessionalLinkResponse from(StudentProfessionalLink link) {
        return new ProfessionalLinkResponse(
                link.getId(),
                link.getStudent().getId(),
                link.getStudent().getFullName(),
                link.getProfessional().getId(),
                link.getProfessional().getFullName(),
                link.getProfessional().getRole(),
                link.getStartedOn(),
                link.getEndedOn(),
                link.isActive(),
                link.getCreatedByUserId(),
                link.getEndedByUserId(),
                link.getEndReason(),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }
}