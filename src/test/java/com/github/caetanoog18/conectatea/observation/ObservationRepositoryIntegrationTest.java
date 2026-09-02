package com.github.caetanoog18.conectatea.observation;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.observation.domain.Observation;
import com.github.caetanoog18.conectatea.observation.infrastructure.ObservationRepository;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@Transactional
class ObservationRepositoryIntegrationTest {
    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Student student;
    private User author;

    @BeforeEach
    void setUp() {
        student = createStudent("OBS-TEST-001");

        author = userRepository.saveAndFlush(
                new User(
                        "Professor de Teste",
                        "observation-teacher@example.com",
                        "temporary-password-hash",
                        UserRole.TEACHER
                )
        );
    }

    @Test
    void shouldPersistObservationWithAuthorAndDates() {
        Instant occurredAt = Instant.parse("2026-01-10T14:00:00Z");

        Observation saved = observationRepository.saveAndFlush(
                new Observation(
                        student,
                        author,
                        ConsentPurpose.EDUCATIONAL_SUPPORT,
                        "Participação em atividade",
                        "Participou da atividade em grupo com apoio visual.",
                        occurredAt
                )
        );

        var observationId = saved.getId();
        var studentId = student.getId();
        var authorId = author.getId();

        entityManager.clear();

        Observation loaded = observationRepository.findById(observationId).orElseThrow();

        assertThat(loaded.getStudent().getId()).isEqualTo(studentId);
        assertThat(loaded.getAuthor().getId()).isEqualTo(authorId);
        assertThat(loaded.getPurpose()).isEqualTo(ConsentPurpose.EDUCATIONAL_SUPPORT);
        assertThat(loaded.getTitle()).isEqualTo("Participação em atividade");
        assertThat(loaded.getContent()).isEqualTo("Participou da atividade em grupo com apoio visual.");
        assertThat(loaded.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getCreatedAt()).isAfterOrEqualTo(occurredAt);
    }

    @Test
    void shouldFilterByStudentAndPurposeAndOrderNewestFirst() {
        Student anotherStudent = createStudent("OBS-TEST-002");

        User psychologist = userRepository.saveAndFlush(
                new User(
                        "Psicologo de Teste",
                        "observation-psychologist@example.com",
                        "temporary-password-hash",
                        UserRole.PSYCHOLOGIST
                )
        );

        saveObservation(
                student, author,
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                "Registro antigo",
                "2026-01-10T14:00:00Z"
        );

        saveObservation(
                student, author,
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                "Registro recente",
                "2026-01-11T14:00:00Z"
        );

        Observation restricted = saveObservation(
                student, psychologist,
                ConsentPurpose.MULTIPROFESSIONAL_MONITORING,
                "Acompanhamento multiprofissional",
                "2026-01-12T14:00:00Z"
        );

        Observation unrelated = saveObservation(
                anotherStudent, author,
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                "Outro estudante",
                "2026-01-13T14:00:00Z"
        );

        var studentId = student.getId();
        var restrictedId = restricted.getId();
        var unrelatedId = unrelated.getId();

        entityManager.clear();

        var page = observationRepository
                .findAllByStudent_IdAndPurposeOrderByOccurredAtDescIdDesc(
                        studentId,
                        ConsentPurpose.EDUCATIONAL_SUPPORT,
                        PageRequest.of(0, 20)
                );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Observation::getTitle)
                .containsExactly("Registro recente", "Registro antigo");

        assertThat(
                observationRepository.findByIdAndStudent_IdAndPurpose(
                        restrictedId,
                        studentId,
                        ConsentPurpose.EDUCATIONAL_SUPPORT
                )
        ).isEmpty();

        assertThat(
                observationRepository.findByIdAndStudent_IdAndPurpose(
                        unrelatedId,
                        studentId,
                        ConsentPurpose.EDUCATIONAL_SUPPORT
                )
        ).isEmpty();
    }

    @Test
    void shouldRejectBlankContent() {
        assertThatThrownBy(
                () -> new Observation(
                        student,
                        author,
                        ConsentPurpose.EDUCATIONAL_SUPPORT,
                        "Atividade",
                        "   ",
                        Instant.parse("2026-01-10T14:00:00Z")
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content is required");
    }

    private Student createStudent(String enrollmentNumber) {
        return studentRepository.saveAndFlush(
                new Student(
                        "Estudante Teste",
                        null,
                        LocalDate.of(2015, 5, 10),
                        enrollmentNumber,
                        2026,
                        "5 ano",
                        "Turma Teste"
                )
        );
    }

    private Observation saveObservation(
            Student targetStudent,
            User targetAuthor,
            ConsentPurpose purpose,
            String title,
            String occurredAt
    ) {
        return observationRepository.saveAndFlush(
                new Observation(
                        targetStudent,
                        targetAuthor,
                        purpose,
                        title,
                        "Conteúdo ficticio para teste de persistencia.",
                        Instant.parse(occurredAt)
                )
        );
    }
}