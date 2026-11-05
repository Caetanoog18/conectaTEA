package com.github.caetanoog18.conectatea.timeline.api.dto;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.observation.domain.Observation;

import java.time.Instant;
import java.util.UUID;

public record TimelineEventResponse(
        UUID id,
        String type,
        UUID studentId,
        UUID authorId,
        String authorName,
        ConsentPurpose purpose,
        String title,
        String content,
        Instant occurredAt,
        Instant recordedAt
) {
    public static TimelineEventResponse from(Observation observation) {
        return new TimelineEventResponse(
                observation.getId(),
                "OBSERVATION",
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