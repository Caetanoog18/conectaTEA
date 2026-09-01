package com.github.caetanoog18.conectatea.consent;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.consent.domain.ConsentStatus;
import com.github.caetanoog18.conectatea.consent.domain.ConsentTerm;
import com.github.caetanoog18.conectatea.consent.infrastructure.ConsentTermRepository;
import com.github.caetanoog18.conectatea.guardian.domain.Guardian;
import com.github.caetanoog18.conectatea.guardian.domain.GuardianRelationship;
import com.github.caetanoog18.conectatea.guardian.domain.StudentGuardian;
import com.github.caetanoog18.conectatea.guardian.infrastructure.GuardianRepository;
import com.github.caetanoog18.conectatea.guardian.infrastructure.StudentGuardianRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@Transactional
class ConsentTermRepositoryIntegrationTest {
    @Autowired
    private ConsentTermRepository consentTermRepository;

    @Autowired
    private StudentGuardianRepository studentGuardianRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveActiveConsentWithPurposes() {
        User administrator = userRepository.saveAndFlush(
                new User(
                        "System Administrator",
                        "admin-consent@conectatea.com",
                        "temporary-password-hash",
                        UserRole.ADMINISTRATOR
                )
        );

        Student student = studentRepository.saveAndFlush(
                new Student(
                        "João Pedro da Silva",
                        null,
                        LocalDate.of(2015, 5, 10),
                        "MAT-CONSENT-001",
                        2026,
                        "5 ano",
                        "Turma B"
                )
        );

        Guardian guardian = guardianRepository.saveAndFlush(
                new Guardian(
                        "Maria da Silva",
                        "52998224725",
                        "maria-consent@conectatea.com",
                        "47999999999",
                        null
                )
        );

        StudentGuardian link = studentGuardianRepository.saveAndFlush(
                new StudentGuardian(
                        student,
                        guardian,
                        GuardianRelationship.LEGAL_GUARDIAN,
                        true,
                        true
                )
        );

        ConsentTerm consent = new ConsentTerm(
                link,
                Set.of(ConsentPurpose.EDUCATIONAL_SUPPORT, ConsentPurpose.MULTIPROFESSIONAL_MONITORING),
                "1.0",
                Instant.parse("2026-09-01T00:00:00Z"),
                LocalDate.of(2027, 9, 1),
                administrator.getId()
        );

        consentTermRepository.saveAndFlush(consent);

        var savedConsent = consentTermRepository
                .findByStudentGuardian_IdAndStatus(link.getId(), ConsentStatus.ACTIVE)
                .orElseThrow();

        assertThat(savedConsent.getId()).isNotNull();
        assertThat(savedConsent.getStatus()).isEqualTo(ConsentStatus.ACTIVE);

        assertThat(savedConsent.getPurposes())
                .containsExactlyInAnyOrder(
                        ConsentPurpose.EDUCATIONAL_SUPPORT,
                        ConsentPurpose.MULTIPROFESSIONAL_MONITORING
                );

        assertThat(savedConsent.getTermsVersion()).isEqualTo("1.0");
        assertThat(savedConsent.isExpired(LocalDate.of(2026, 9, 1))).isFalse();
    }
}