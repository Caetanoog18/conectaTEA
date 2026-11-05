CREATE TABLE students
(
    id                UUID PRIMARY KEY,
    full_name         VARCHAR(120)             NOT NULL,
    preferred_name    VARCHAR(120),
    birth_date        DATE                     NOT NULL,
    enrollment_number VARCHAR(50)              NOT NULL,
    school_year       INTEGER                  NOT NULL,
    grade_level       VARCHAR(50)              NOT NULL,
    class_name        VARCHAR(30),
    active            BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_students_school_year
        CHECK (school_year BETWEEN 2000 AND 2100)
);

CREATE UNIQUE INDEX uk_students_enrollment_number_lower
    ON students (LOWER(enrollment_number));