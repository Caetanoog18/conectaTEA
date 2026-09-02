package com.github.caetanoog18.conectatea.report;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.careteam.domain.StudentProfessionalLink;
import com.github.caetanoog18.conectatea.careteam.infrastructure.StudentProfessionalLinkRepository;
import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
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
import com.github.caetanoog18.conectatea.report.application.ReportAccessService;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

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
class ReportAccessIntegrationTest {
    @Autowired private ReportAccessService reportAccessService;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private GuardianRepository guardianRepository;
    @Autowired private StudentGuardianRepository guardianLinkRepository;
    @Autowired private StudentProfessionalLinkRepository professionalLinkRepository;
    @Autowired private ConsentTermRepository consentRepository;

    private User administrator;
    private User teacher;
    private Student student;
    private StudentGuardian guardianLink;

    @BeforeEach
    void setUp() {
        administrator = persistUser(UserRole.ADMINISTRATOR);
        teacher = persistUser(UserRole.TEACHER);

        student = studentRepository.saveAndFlush(
                new Student(
                        "Estudante Teste Relatorio",
                        null,
                        LocalDate.of(2015, 5, 10),
                        "REPORT-" + UUID.randomUUID(),
                        2026,
                        "5 ano",
                        "Turma Teste"
                )
        );

        guardianLink = createGuardianLink();

        linkProfessional(teacher);
    }

    @Test
    void teacherWithAllPurposesShouldBeAuthorized() {
        grant(guardianLink, educationalReportPurposes());

        var access = reportAccessService.requireGenerationAccess(student.getId(), teacher.getEmail());

        assertThat(access.student().getId()).isEqualTo(student.getId());
        assertThat(access.requester().getId()).isEqualTo(teacher.getId());
        assertThat(access.purpose()).isEqualTo(ConsentPurpose.EDUCATIONAL_SUPPORT);
    }

    @Test
    void observationReadConsentShouldNotAuthorizeReports() {
        grant(guardianLink, observationReadPurposes());

        assertTeacherDenied();
    }

    @Test
    void reportPurposeAloneShouldNotAuthorizeUnderlyingData() {
        grant(guardianLink, Set.of(ConsentPurpose.REPORT_GENERATION, ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM));

        assertTeacherDenied();
    }

    @Test
    void purposesFromDifferentTermsShouldNotBeCombined() {
        grant(guardianLink, observationReadPurposes());

        StudentGuardian secondLink = createGuardianLink();

        grant(secondLink, Set.of(ConsentPurpose.REPORT_GENERATION, ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM));

        assertTeacherDenied();
    }

    @Test
    void expiredReportConsentShouldNotAuthorizeGeneration() {
        grant(guardianLink, observationReadPurposes());

        StudentGuardian secondLink = createGuardianLink();

        consentRepository.saveAndFlush(
                new ConsentTerm(
                        secondLink,
                        educationalReportPurposes(),
                        "test-1.0",
                        Instant.now().minusSeconds(3 * 86400L),
                        today().minusDays(1),
                        administrator.getId()
                )
        );

        assertTeacherDenied();
    }

    @Test
    void revokedReportConsentShouldBlockGenerationEvenIfReadRemainsAllowed() {
        grant(guardianLink, observationReadPurposes());

        StudentGuardian secondLink = createGuardianLink();

        ConsentTerm reportConsent = grant(
                secondLink,
                educationalReportPurposes()
        );

        reportAccessService.requireGenerationAccess(
                student.getId(),
                teacher.getEmail()
        );

        reportConsent.revoke(
                Instant.now(),
                administrator.getId(),
                "Report permission withdrawn"
        );

        consentRepository.saveAndFlush(reportConsent);

        assertTeacherDenied();
    }

    @Test
    void inactiveProfessionalShouldBeRejected() {
        grant(guardianLink, educationalReportPurposes());

        teacher.deactivate();
        userRepository.saveAndFlush(teacher);

        assertTeacherDenied();
    }

    @Test
    void administratorShouldNotBypassProfessionalAuthorization() {
        grant(guardianLink, educationalReportPurposes());

        assertThatThrownBy(() -> reportAccessService.requireGenerationAccess(student.getId(), administrator.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void physicianShouldUseMultiprofessionalPurpose() {
        User physician = persistUser(UserRole.PHYSICIAN);
        linkProfessional(physician);

        grant(
                guardianLink,
                Set.of(
                        ConsentPurpose.MULTIPROFESSIONAL_MONITORING,
                        ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM,
                        ConsentPurpose.REPORT_GENERATION
                )
        );

        var access = reportAccessService.requireGenerationAccess(student.getId(), physician.getEmail());

        assertThat(access.requester().getId()).isEqualTo(physician.getId());
        assertThat(access.purpose()).isEqualTo(ConsentPurpose.MULTIPROFESSIONAL_MONITORING);
    }

    private void assertTeacherDenied() {
        assertThatThrownBy(() -> reportAccessService.requireGenerationAccess(student.getId(), teacher.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    private User persistUser(UserRole role) {
        return userRepository.saveAndFlush(
                new User(
                        "Usuario Teste",
                        "report-" + UUID.randomUUID() + "@example.com",
                        "temporary-password-hash",
                        role
                )
        );
    }

    private void linkProfessional(User professional) {
        professionalLinkRepository.saveAndFlush(
                new StudentProfessionalLink(
                        student,
                        professional,
                        today().minusDays(1),
                        administrator.getId()
                )
        );
    }

    private StudentGuardian createGuardianLink() {
        Guardian guardian = guardianRepository.saveAndFlush(
                new Guardian(
                        "Responsavel Teste",
                        null,
                        "report-guardian-" + UUID.randomUUID() + "@example.com",
                        "00000000000",
                        null
                )
        );

        return guardianLinkRepository.saveAndFlush(
                new StudentGuardian(
                        student,
                        guardian,
                        GuardianRelationship.LEGAL_GUARDIAN,
                        true,
                        false
                )
        );
    }

    private ConsentTerm grant(StudentGuardian link, Set<ConsentPurpose> purposes) {
        return consentRepository.saveAndFlush(
                new ConsentTerm(
                        link,
                        purposes,
                        "test-1.0",
                        Instant.now().minusSeconds(60),
                        today().plusDays(30),
                        administrator.getId()
                )
        );
    }

    private static Set<ConsentPurpose> observationReadPurposes() {
        return Set.of(
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
        );
    }

    private static Set<ConsentPurpose> educationalReportPurposes() {
        return Set.of(
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM,
                ConsentPurpose.REPORT_GENERATION
        );
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}