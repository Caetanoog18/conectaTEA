ALTER TABLE audit_events
DROP
CONSTRAINT ck_audit_events_action;

ALTER TABLE audit_events
    ADD CONSTRAINT ck_audit_events_action
        CHECK (
    action IN (
    'OBSERVATION_CREATE',
    'OBSERVATION_LIST',
    'OBSERVATION_READ',
    'TIMELINE_READ',
    'AUDIT_EVENTS_LIST'
    )
    );

CREATE INDEX idx_audit_events_date
    ON audit_events (occurred_at DESC, id DESC);