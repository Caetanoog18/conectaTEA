package com.github.caetanoog18.conectatea.guardian;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.guardian.domain.Guardian;
import com.github.caetanoog18.conectatea.guardian.domain.GuardianRelationship;
import com.github.caetanoog18.conectatea.guardian.domain.StudentGuardian;
import com.github.caetanoog18.conectatea.guardian.infrastructure.GuardianRepository;
import com.github.caetanoog18.conectatea.guardian.infrastructure.StudentGuardianRepository;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@Transactional
class GuardianRepositoryIntegrationTest {
    @Autowired
    private GuardianRepository guardianRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudentGuardianRepository studentGuardianRepository;

    @Test
    void shouldPersistGuardianAndLinkToStudent() {
        Student student = studentRepository.saveAndFlush(
                new Student(
                        "João da Silva",
                        "João",
                        LocalDate.of(2015, 3, 12),
                        "MAT-2026-001",
                        2026,
                        "5 ano",
                        "Turma A"
                )
        );
        Guardian guardian = guardianRepository.saveAndFlush(
                new Guardian(
                        "Maria da Silva",
                        "123.456.789-00",
                        "MARIA@EXAMPLE.COM",
                        "(47) 99999-9999",
                        null
                )
        );


        StudentGuardian link =
                studentGuardianRepository.saveAndFlush(
                        new StudentGuardian(
                                student,
                                guardian,
                                GuardianRelationship.MOTHER,
                                true,
                                true
                        )
                );

        assertThat(guardian.getId()).isNotNull();
        assertThat(guardian.getCpf()).isEqualTo("12345678900");
        assertThat(guardian.getEmail())
                .isEqualTo("maria@example.com");
        assertThat(guardian.getPhone())
                .isEqualTo("47999999999");

        assertThat(link.getId()).isNotNull();
        assertThat(link.getStudent().getId())
                .isEqualTo(student.getId());
        assertThat(link.getGuardian().getId())
                .isEqualTo(guardian.getId());
        assertThat(link.getRelationship())
                .isEqualTo(GuardianRelationship.MOTHER);
        assertThat(link.isLegalGuardian()).isTrue();
        assertThat(link.isPrimaryContact()).isTrue();
    }
}