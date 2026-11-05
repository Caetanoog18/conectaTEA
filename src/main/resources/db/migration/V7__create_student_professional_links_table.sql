CREATE TABLE student_professional_links
(
    id                   UUID PRIMARY KEY,
    student_id           UUID                     NOT NULL,
    professional_user_id UUID                     NOT NULL,
    started_on           DATE                     NOT NULL,
    ended_on             DATE,
    active               BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_by_user_id   UUID                     NOT NULL,
    ended_by_user_id     UUID,
    end_reason           VARCHAR(500),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
                                                           DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL
                                                           DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_professional_links_student
        FOREIGN KEY (student_id)
            REFERENCES students (id),

    CONSTRAINT fk_professional_links_professional
        FOREIGN KEY (professional_user_id)
            REFERENCES users (id),

    CONSTRAINT fk_professional_links_created_by
        FOREIGN KEY (created_by_user_id)
            REFERENCES users (id),

    CONSTRAINT fk_professional_links_ended_by
        FOREIGN KEY (ended_by_user_id)
            REFERENCES users (id),

    CONSTRAINT ck_professional_links_dates
        CHECK (
            ended_on IS NULL
                OR ended_on >= started_on
            ),

    CONSTRAINT ck_professional_links_status
        CHECK (
            (
                active = TRUE
                    AND ended_on IS NULL
                    AND ended_by_user_id IS NULL
                    AND end_reason IS NULL
                )
                OR
            (
                active = FALSE
                    AND ended_on IS NOT NULL
                    AND ended_by_user_id IS NOT NULL
                    AND end_reason IS NOT NULL
                    AND LENGTH(TRIM(end_reason)) > 0
                )
            )
);

CREATE UNIQUE INDEX uk_professional_links_active
    ON student_professional_links (
                                   student_id,
                                   professional_user_id
        ) WHERE active = TRUE;

CREATE INDEX idx_professional_links_student
    ON student_professional_links (student_id);

CREATE INDEX idx_professional_links_professional
    ON student_professional_links (professional_user_id);