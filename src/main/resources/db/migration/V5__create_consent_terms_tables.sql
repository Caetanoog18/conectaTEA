CREATE TABLE consent_terms
(
    id                  UUID PRIMARY KEY,
    student_guardian_id UUID                     NOT NULL,
    status              VARCHAR(20)              NOT NULL,
    terms_version       VARCHAR(20)              NOT NULL,
    granted_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until         DATE,
    recorded_by_user_id UUID                     NOT NULL,
    revoked_at          TIMESTAMP WITH TIME ZONE,
    revoked_by_user_id  UUID,
    revocation_reason   VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_consent_terms_student_guardian
        FOREIGN KEY (student_guardian_id)
            REFERENCES student_guardians (id),

    CONSTRAINT fk_consent_terms_recorded_by
        FOREIGN KEY (recorded_by_user_id)
            REFERENCES users (id),

    CONSTRAINT fk_consent_terms_revoked_by
        FOREIGN KEY (revoked_by_user_id)
            REFERENCES users (id),

    CONSTRAINT ck_consent_terms_status
        CHECK (status IN ('ACTIVE', 'REVOKED')),

    CONSTRAINT ck_consent_terms_revocation
        CHECK (
            (
                status = 'ACTIVE'
                    AND revoked_at IS NULL
                    AND revoked_by_user_id IS NULL
                    AND revocation_reason IS NULL
                )
                OR
            (
                status = 'REVOKED'
                    AND revoked_at IS NOT NULL
                    AND revoked_by_user_id IS NOT NULL
                )
            ),

    CONSTRAINT ck_consent_terms_validity
        CHECK (
            valid_until IS NULL
                OR valid_until >= CAST(granted_at AS DATE)
            )
);

CREATE UNIQUE INDEX uk_consent_terms_active_link
    ON consent_terms (student_guardian_id) WHERE status = 'ACTIVE';

CREATE INDEX idx_consent_terms_student_guardian
    ON consent_terms (student_guardian_id);


CREATE TABLE consent_term_purposes
(
    consent_term_id UUID        NOT NULL,
    purpose         VARCHAR(60) NOT NULL,

    CONSTRAINT pk_consent_term_purposes
        PRIMARY KEY (consent_term_id, purpose),

    CONSTRAINT fk_consent_term_purposes_consent
        FOREIGN KEY (consent_term_id)
            REFERENCES consent_terms (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_consent_term_purposes_purpose
        CHECK (
            purpose IN (
                        'EDUCATIONAL_SUPPORT',
                        'MULTIPROFESSIONAL_MONITORING',
                        'INFORMATION_SHARING_WITH_CARE_TEAM',
                        'REPORT_GENERATION',
                        'IMAGE_AND_MEDIA_USE'
                )
            )
);