package com.github.caetanoog18.conectatea.student;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
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
class StudentRepositoryIntegrationTest {
    @Autowired
    private StudentRepository studentRepository;

    @Test
    void shouldPersistAndFindStudentByEnrollmentNumberIgnoringCase() {
        Student student = new Student(
                "João da Silva",
                "João",
                LocalDate.of(2015, 3, 12),
                "mat-2026-001",
                2026,
                "5 ano",
                "Turma A"
        );

        Student savedStudent =
                studentRepository.saveAndFlush(student);

        assertThat(savedStudent.getId()).isNotNull();
        assertThat(savedStudent.getEnrollmentNumber())
                .isEqualTo("MAT-2026-001");
        assertThat(savedStudent.isActive()).isTrue();

        assertThat(
                studentRepository.findByEnrollmentNumberIgnoreCase(
                        "mat-2026-001"
                )
        ).contains(savedStudent);
    }
}