DO
$$
BEGIN
    IF EXISTS (
        SELECT 1 FROM students WHERE NOT is_valid_cpf(cpf)
    ) OR EXISTS (
        SELECT 1 FROM guardians WHERE NOT is_valid_cpf(cpf)
    ) OR EXISTS (
        SELECT 1 FROM users WHERE NOT is_valid_cpf(cpf)
    ) THEN
        RAISE EXCEPTION
            'Fill all existing records with valid CPFs before applying this migration';
END IF;

    IF EXISTS (
        SELECT 1
        FROM guardians g
        JOIN users u ON u.id = g.user_id
        WHERE g.cpf IS DISTINCT FROM u.cpf
    ) THEN
        RAISE EXCEPTION
            'Guardian CPF must match the associated user CPF';
END IF;
END;
$$;

ALTER TABLE students
    ALTER COLUMN cpf SET NOT NULL;

ALTER TABLE guardians
    ALTER COLUMN cpf SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN cpf SET NOT NULL;

ALTER TABLE students
    ADD CONSTRAINT ck_students_valid_cpf
        CHECK (is_valid_cpf(cpf));

ALTER TABLE guardians
    ADD CONSTRAINT ck_guardians_valid_cpf
        CHECK (is_valid_cpf(cpf));

ALTER TABLE users
    ADD CONSTRAINT ck_users_valid_cpf
        CHECK (is_valid_cpf(cpf));