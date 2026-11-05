CREATE TABLE guardians
(
    id         UUID PRIMARY KEY,
    user_id    UUID,
    full_name  VARCHAR(120)             NOT NULL,
    cpf        VARCHAR(11),
    email      VARCHAR(254),
    phone      VARCHAR(20)              NOT NULL,
    active     BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_guardians_user
        FOREIGN KEY (user_id)
            REFERENCES users (id),

    CONSTRAINT uk_guardians_user
        UNIQUE (user_id)
);

CREATE UNIQUE INDEX uk_guardians_cpf
    ON guardians (cpf) WHERE cpf IS NOT NULL;

CREATE TABLE student_guardians
(
    id              UUID PRIMARY KEY,
    student_id      UUID                     NOT NULL,
    guardian_id     UUID                     NOT NULL,
    relationship    VARCHAR(30)              NOT NULL,
    legal_guardian  BOOLEAN                  NOT NULL DEFAULT FALSE,
    primary_contact BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_student_guardians_student
        FOREIGN KEY (student_id)
            REFERENCES students (id),

    CONSTRAINT fk_student_guardians_guardian
        FOREIGN KEY (guardian_id)
            REFERENCES guardians (id),

    CONSTRAINT uk_student_guardians_link
        UNIQUE (student_id, guardian_id),

    CONSTRAINT ck_student_guardians_relationship
        CHECK (
            relationship IN (
                             'MOTHER',
                             'FATHER',
                             'STEPMOTHER',
                             'STEPFATHER',
                             'GRANDMOTHER',
                             'GRANDFATHER',
                             'SIBLING',
                             'LEGAL_GUARDIAN',
                             'OTHER'
                )
            )
);

CREATE INDEX idx_student_guardians_student
    ON student_guardians (student_id);

CREATE INDEX idx_student_guardians_guardian
    ON student_guardians (guardian_id);

CREATE UNIQUE INDEX uk_student_guardians_primary_contact
    ON student_guardians (student_id) WHERE primary_contact = TRUE;