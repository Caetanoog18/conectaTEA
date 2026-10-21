package com.github.caetanoog18.conectatea.careteam.api.dto;

import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;

import java.util.UUID;

public record ProfessionalOptionResponse(
        UUID id,
        String fullName,
        String email,
        UserRole role
) {
    public static ProfessionalOptionResponse from(User user) {
        return new ProfessionalOptionResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );
    }
}