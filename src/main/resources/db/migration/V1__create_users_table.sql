CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    full_name     VARCHAR(120)             NOT NULL,
    email         VARCHAR(254)             NOT NULL,
    password_hash VARCHAR(60)              NOT NULL,
    role          VARCHAR(40)              NOT NULL,
    active        BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_users_role CHECK (
        role IN (
                 'ADMINISTRATOR',
                 'PEDAGOGICAL_COORDINATOR',
                 'TEACHER',
                 'AEE_TEACHER',
                 'PSYCHOLOGIST',
                 'PHYSICIAN',
                 'LEGAL_GUARDIAN'
            )
        )
);

CREATE UNIQUE INDEX uk_users_email_lower
    ON users (LOWER(email));