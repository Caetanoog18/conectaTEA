package com.github.caetanoog18.conectatea.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private AuditOutcome outcome;

    @Column(name = "student_id", updatable = false)
    private UUID studentId;

    @Column(name = "resource_id", updatable = false)
    private UUID resourceId;

    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(
            UUID actorUserId,
            AuditAction action,
            AuditOutcome outcome,
            UUID studentId,
            UUID resourceId,
            UUID requestId
    ) {
        this.actorUserId = actorUserId;

        this.action = Objects.requireNonNull(action, "Audit action is required");

        this.outcome = Objects.requireNonNull(outcome, "Audit outcome is required");

        this.studentId = studentId;
        this.resourceId = resourceId;

        this.requestId = Objects.requireNonNull(requestId, "Request ID is required");
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}