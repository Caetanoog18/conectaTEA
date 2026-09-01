package com.github.caetanoog18.conectatea.guardian.api.dto;

import com.github.caetanoog18.conectatea.guardian.domain.GuardianRelationship;
import jakarta.validation.constraints.NotNull;

public record UpdateStudentGuardianLinkRequest(
        @NotNull(message = "Relationship is required")
        GuardianRelationship relationship,

        @NotNull(message = "Legal guardian status is required")
        Boolean legalGuardian,

        @NotNull(message = "Primary contact status is required")
        Boolean primaryContact
) {
}