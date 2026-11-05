package com.github.caetanoog18.conectatea.shared.domain;

import com.github.caetanoog18.conectatea.guardian.api.dto.GuardianRequest;
import com.github.caetanoog18.conectatea.identity.api.dto.CreateUserRequest;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.student.api.dto.StudentRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CpfRequestValidationTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ",
            "11111111111",
            "52998224724"
    })
    void shouldRejectMissingOrInvalidCpfInAllRegistrations(String cpf) {
        Object[] requests = {
                new StudentRequest(
                        "Estudante de Teste",
                        null,
                        LocalDate.of(2015, 3, 12),
                        "MAT-TEST",
                        2026,
                        "5º ano",
                        "Turma A",
                        cpf
                ),
                new GuardianRequest(
                        "Responsável de Teste",
                        cpf,
                        "guardian@example.com",
                        "47999999999",
                        null
                ),
                new CreateUserRequest(
                        "Professor de Teste",
                        "teacher@example.com",
                        "StrongPassword123!",
                        UserRole.TEACHER,
                        cpf
                )
        };

        for (Object request : requests) {
            assertThat(VALIDATOR.validate(request))
                    .anySatisfy(violation ->
                            assertThat(violation.getPropertyPath().toString()).isEqualTo("cpf")
                    );
        }
    }
}