CREATE FUNCTION is_valid_cpf(p_cpf TEXT)
    RETURNS BOOLEAN
    LANGUAGE plpgsql
    IMMUTABLE
AS
$$
DECLARE
total       INTEGER;
    first_digit
INTEGER;
    last_digit
INTEGER;
    i
INTEGER;
BEGIN
    IF
p_cpf IS NULL OR p_cpf !~ '^[0-9]{11}$' THEN
        RETURN FALSE;
END IF;

    IF
p_cpf = REPEAT(SUBSTRING(p_cpf FROM 1 FOR 1), 11) THEN
        RETURN FALSE;
END IF;

    total
:= 0;

FOR i IN 1..9 LOOP
        total := total
            + SUBSTRING(p_cpf FROM i FOR 1)::INTEGER * (11 - i);
END LOOP;

    first_digit
:= (total * 10) % 11;

    IF
first_digit = 10 THEN
        first_digit := 0;
END IF;

    total
:= 0;

FOR i IN 1..10 LOOP
        total := total
            + SUBSTRING(p_cpf FROM i FOR 1)::INTEGER * (12 - i);
END LOOP;

    last_digit
:= (total * 10) % 11;

    IF
last_digit = 10 THEN
        last_digit := 0;
END IF;

RETURN first_digit = SUBSTRING(p_cpf FROM 10 FOR 1)::INTEGER
       AND last_digit = SUBSTRING(p_cpf FROM 11 FOR 1)::INTEGER;
END;
$$;