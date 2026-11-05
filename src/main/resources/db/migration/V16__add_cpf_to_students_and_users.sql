ALTER TABLE students
    ADD COLUMN cpf VARCHAR(11);

ALTER TABLE users
    ADD COLUMN cpf VARCHAR(11);

ALTER TABLE students
    ADD CONSTRAINT ck_students_cpf_format
        CHECK (
            cpf IS NULL
                OR cpf ~ '^[0-9]{11}$'
    );

ALTER TABLE users
    ADD CONSTRAINT ck_users_cpf_format
        CHECK (
            cpf IS NULL
                OR cpf ~ '^[0-9]{11}$'
    );

CREATE UNIQUE INDEX uk_students_cpf
    ON students (cpf) WHERE cpf IS NOT NULL;

CREATE UNIQUE INDEX uk_users_cpf
    ON users (cpf) WHERE cpf IS NOT NULL;