package com.github.caetanoog18.conectatea.careteam;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.careteam.domain.StudentProfessionalLink;
import com.github.caetanoog18.conectatea.careteam.infrastructure.StudentProfessionalLinkRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
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
class StudentProfessionalLinkRepositoryIntegrationTest {
    @Autowired
    private StudentProfessionalLinkRepository linkRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveActiveProfessionalLink() {
        User administrator = persistUser(
                "Care Team Administrator",
                "careteam-admin@conectatea.com",
                UserRole.ADMINISTRATOR
        );

        User psychologist = persistUser(
                "Ana Psicóloga",
                "ana.psicologa@conectatea.com",
                UserRole.PSYCHOLOGIST
        );

        Student student = persistStudent();

        StudentProfessionalLink link =
                linkRepository.saveAndFlush(
                        new StudentProfessionalLink(
                                student,
                                psychologist,
                                LocalDate.of(2026, 9, 1),
                                administrator.getId()
                        )
                );

        assertThat(link.getId()).isNotNull();
        assertThat(link.isActive()).isTrue();

        assertThat(
                linkRepository
                        .existsByStudent_IdAndProfessional_IdAndActiveTrue(
                                student.getId(),
                                psychologist.getId()
                        )
        ).isTrue();
    }

    @Test
    void shouldPreserveEndedProfessionalLink() {
        User administrator = persistUser(
                "Care Team Administrator",
                "careteam-admin@conectatea.com",
                UserRole.ADMINISTRATOR
        );

        User physician = persistUser(
                "Carlos Médico",
                "carlos.medico@conectatea.com",
                UserRole.PHYSICIAN
        );

        Student student = persistStudent();

        StudentProfessionalLink link =
                new StudentProfessionalLink(
                        student,
                        physician,
                        LocalDate.of(2026, 9, 1),
                        administrator.getId()
                );

        link.end(
                LocalDate.of(2026, 12, 1),
                administrator.getId(),
                "Professional no longer provides care"
        );

        linkRepository.saveAndFlush(link);

        assertThat(link.isActive()).isFalse();
        assertThat(link.getEndedOn()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(link.getEndReason()).isEqualTo("Professional no longer provides care");
        assertThat(linkRepository.count()).isEqualTo(1);
    }

    private User persistUser(String fullName, String email, UserRole role) {
        return userRepository.saveAndFlush(
                new User(
                        fullName,
                        email,
                        "temporary-password-hash",
                        role
                )
        );
    }

    private Student persistStudent() {
        return studentRepository.saveAndFlush(
                new Student(
                        "João Pedro da Silva",
                        "João",
                        LocalDate.of(2015, 5, 10),
                        "MAT-CARETEAM-001",
                        2026,
                        "5 ano",
                        "Turma B"
                )
        );
    }
}