package com.github.caetanoog18.conectatea.audit.api.dto;

import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditEvent;
import com.github.caetanoog18.conectatea.audit.domain.AuditOutcome;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID actorUserId,
        AuditAction action,
        AuditOutcome outcome,
        UUID studentId,
        UUID resourceId,
        UUID requestId,
        Instant occurredAt
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActorUserId(),
                event.getAction(),
                event.getOutcome(),
                event.getStudentId(),
                event.getResourceId(),
                event.getRequestId(),
                event.getOccurredAt()
        );
    }
}