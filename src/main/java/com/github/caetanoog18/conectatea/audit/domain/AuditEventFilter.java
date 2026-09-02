package com.github.caetanoog18.conectatea.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditEventFilter(
        UUID studentId,
        UUID actorUserId,
        UUID requestId,
        AuditAction action,
        AuditOutcome outcome,
        Instant from,
        Instant to
) {
}