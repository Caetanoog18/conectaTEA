package com.github.caetanoog18.conectatea.guardian.api.dto;

import com.github.caetanoog18.conectatea.guardian.domain.GuardianRelationship;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateStudentGuardianLinkRequest(
        @NotNull(message = "Guardian ID is required")
        UUID guardianId,

        @NotNull(message = "Relationship is required")
        GuardianRelationship relationship,

        @NotNull(message = "Legal guardian status is required")
        Boolean legalGuardian,

        @NotNull(message = "Primary contact status is required")
        Boolean primaryContact
) {
}