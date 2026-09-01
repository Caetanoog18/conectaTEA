package com.github.caetanoog18.conectatea.guardian.api.dto;


import com.github.caetanoog18.conectatea.guardian.domain.Guardian;

import java.util.UUID;
import java.time.Instant;

public record GuardianResponse(
        UUID id,
        UUID userId,
        String fullName,
        String cpf,
        String email,
        String phone,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static GuardianResponse from(Guardian guardian) {
        return new GuardianResponse(
                guardian.getId(),
                guardian.getUserId(),
                guardian.getFullName(),
                guardian.getCpf(),
                guardian.getEmail(),
                guardian.getPhone(),
                guardian.isActive(),
                guardian.getCreatedAt(),
                guardian.getUpdatedAt()
        );
    }
}
