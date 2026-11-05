CREATE TABLE observations
(
    id             UUID PRIMARY KEY,
    student_id     UUID                     NOT NULL,
    author_user_id UUID                     NOT NULL,
    purpose        VARCHAR(40)              NOT NULL,
    title          VARCHAR(120)             NOT NULL,
    content        VARCHAR(5000)            NOT NULL,
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_observations_student
        FOREIGN KEY (student_id)
            REFERENCES students (id),

    CONSTRAINT fk_observations_author
        FOREIGN KEY (author_user_id)
            REFERENCES users (id),

    CONSTRAINT ck_observations_purpose
        CHECK (
            purpose IN (
                        'EDUCATIONAL_SUPPORT',
                        'MULTIPROFESSIONAL_MONITORING'
                )
            ),

    CONSTRAINT ck_observations_title
        CHECK (LENGTH(TRIM(title)) > 0),

    CONSTRAINT ck_observations_content
        CHECK (LENGTH(TRIM(content)) > 0),

    CONSTRAINT ck_observations_dates
        CHECK (occurred_at <= created_at)
);

CREATE INDEX idx_observations_student_purpose_date
    ON observations (
                     student_id,
                     purpose,
                     occurred_at DESC,
                     id DESC
        );

CREATE INDEX idx_observations_author
    ON observations (author_user_id);