package com.github.caetanoog18.conectatea.identity.api.dto;


import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt

) {
    public static UserResponse from(User user){
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
