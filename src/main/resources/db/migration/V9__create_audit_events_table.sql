CREATE TABLE audit_events
(
    id            UUID PRIMARY KEY,
    actor_user_id UUID,
    action        VARCHAR(50)              NOT NULL,
    outcome       VARCHAR(20)              NOT NULL,
    student_id    UUID,
    resource_id   UUID,
    request_id    UUID                     NOT NULL,
    occurred_at   TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_audit_events_action
        CHECK (
            action IN (
            'OBSERVATION_CREATE',
            'OBSERVATION_LIST',
            'OBSERVATION_READ',
            'TIMELINE_READ'
            )
) ,

    CONSTRAINT ck_audit_events_outcome
        CHECK (
            outcome IN (
                'SUCCESS',
                'DENIED',
                'FAILURE'
            )
        )
);

CREATE INDEX idx_audit_events_student_date
    ON audit_events (
                     student_id,
                     occurred_at DESC,
                     id DESC
        );

CREATE INDEX idx_audit_events_actor_date
    ON audit_events (
                     actor_user_id,
                     occurred_at DESC,
                     id DESC
        );

CREATE INDEX idx_audit_events_request
    ON audit_events (request_id);