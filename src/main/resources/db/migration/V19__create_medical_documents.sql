CREATE TABLE medical_documents
(
    id                  UUID PRIMARY KEY,
    student_id          UUID         NOT NULL REFERENCES students (id),
    uploaded_by_user_id UUID         NOT NULL REFERENCES users (id),
    title               VARCHAR(120) NOT NULL CHECK (LENGTH(BTRIM(title)) > 0),
    issued_on           DATE         NOT NULL,
    size_bytes          BIGINT       NOT NULL CHECK (size_bytes BETWEEN 1 AND 10485760),
    content             BYTEA        NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_medical_document_size CHECK (
        OCTET_LENGTH(content) = size_bytes
        )
);

CREATE INDEX ix_medical_documents_student_created
    ON medical_documents (student_id, created_at DESC, id DESC);

ALTER TABLE audit_events
DROP
CONSTRAINT ck_audit_events_action;

ALTER TABLE audit_events
    ADD CONSTRAINT ck_audit_events_action CHECK (
    action IN (
    'OBSERVATION_CREATE',
    'OBSERVATION_LIST',
    'OBSERVATION_READ',
    'TIMELINE_READ',
    'AUDIT_EVENTS_LIST',
    'REPORT_GENERATE',
    'REPORT_PDF_EXPORT',
    'USER_CREATE',
    'USER_STATUS_UPDATE',
    'CONSENT_CREATE',
    'CONSENT_REVOKE',
    'STUDENT_CREATE',
    'STUDENT_UPDATE',
    'STUDENT_STATUS_UPDATE',
    'GUARDIAN_CREATE',
    'GUARDIAN_UPDATE',
    'GUARDIAN_STATUS_UPDATE',
    'STUDENT_GUARDIAN_LINK_CREATE',
    'STUDENT_GUARDIAN_LINK_UPDATE',
    'STUDENT_GUARDIAN_LINK_DELETE',
    'CARE_TEAM_LINK_CREATE',
    'CARE_TEAM_LINK_END',
    'MEDICAL_DOCUMENT_UPLOAD',
    'MEDICAL_DOCUMENT_LIST',
    'MEDICAL_DOCUMENT_DOWNLOAD'
    )
    );