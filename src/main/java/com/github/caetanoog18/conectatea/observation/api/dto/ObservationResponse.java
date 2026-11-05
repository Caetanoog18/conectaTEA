package com.github.caetanoog18.conectatea.observation.api.dto;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.observation.domain.Observation;

import java.time.Instant;
import java.util.UUID;

public record ObservationResponse(
        UUID id,
        UUID studentId,
        UUID authorId,
        String authorName,
        ConsentPurpose purpose,
        String title,
        String content,
        Instant occurredAt,
        Instant createdAt
) {
    public static ObservationResponse from(Observation observation) {
        return new ObservationResponse(
                observation.getId(),
                observation.getStudent().getId(),
                observation.getAuthor().getId(),
                observation.getAuthor().getFullName(),
                observation.getPurpose(),
                observation.getTitle(),
                observation.getContent(),
                observation.getOccurredAt(),
                observation.getCreatedAt()
        );
    }
}