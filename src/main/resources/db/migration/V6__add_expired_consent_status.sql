ALTER TABLE consent_terms
DROP
CONSTRAINT ck_consent_terms_status;

ALTER TABLE consent_terms
    ADD CONSTRAINT ck_consent_terms_status
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'));

ALTER TABLE consent_terms
DROP
CONSTRAINT ck_consent_terms_revocation;

ALTER TABLE consent_terms
    ADD CONSTRAINT ck_consent_terms_revocation
        CHECK (
            (
                status IN ('ACTIVE', 'EXPIRED')
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
            );