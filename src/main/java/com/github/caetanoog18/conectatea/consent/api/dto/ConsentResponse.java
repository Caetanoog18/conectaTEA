package com.github.caetanoog18.conectatea.consent.api.dto;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.consent.domain.ConsentStatus;
import com.github.caetanoog18.conectatea.consent.domain.ConsentTerm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ConsentResponse(
        UUID id,
        UUID studentGuardianId,
        UUID studentId,
        String studentName,
        UUID guardianId,
        String guardianName,
        ConsentStatus status,
        List<ConsentPurpose> purposes,
        String termsVersion,
        Instant grantedAt,
        LocalDate validUntil,
        UUID recordedByUserId,
        Instant revokedAt,
        UUID revokedByUserId,
        String revocationReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConsentResponse from(ConsentTerm consent) {
        return new ConsentResponse(
                consent.getId(),
                consent.getStudentGuardian().getId(),
                consent.getStudentGuardian().getStudent().getId(),
                consent.getStudentGuardian().getStudent().getFullName(),
                consent.getStudentGuardian().getGuardian().getId(),
                consent.getStudentGuardian().getGuardian().getFullName(),
                consent.getStatus(),
                consent.getPurposes().stream().sorted().toList(),
                consent.getTermsVersion(),
                consent.getGrantedAt(),
                consent.getValidUntil(),
                consent.getRecordedByUserId(),
                consent.getRevokedAt(),
                consent.getRevokedByUserId(),
                consent.getRevocationReason(),
                consent.getCreatedAt(),
                consent.getUpdatedAt()
        );
    }
}