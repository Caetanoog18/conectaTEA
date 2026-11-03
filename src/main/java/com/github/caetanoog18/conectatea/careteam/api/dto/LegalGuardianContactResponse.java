package com.github.caetanoog18.conectatea.careteam.api.dto;

import com.github.caetanoog18.conectatea.guardian.domain.GuardianRelationship;
import com.github.caetanoog18.conectatea.guardian.domain.StudentGuardian;

import java.util.UUID;

public record LegalGuardianContactResponse(
        UUID guardianId,
        String fullName,
        GuardianRelationship relationship,
        boolean primaryContact,
        String email,
        String phone
) {
    public static LegalGuardianContactResponse from(StudentGuardian link) {
        var guardian = link.getGuardian();

        return new LegalGuardianContactResponse(
                guardian.getId(),
                guardian.getFullName(),
                link.getRelationship(),
                link.isPrimaryContact(),
                guardian.getEmail(),
                guardian.getPhone()
        );
    }
}