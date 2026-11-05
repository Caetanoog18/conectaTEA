package com.github.caetanoog18.conectatea.careteam;

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
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import com.github.caetanoog18.conectatea.support.TestCpf;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password=",
        "app.bootstrap.admin.cpf="
})
@AutoConfigureMockMvc
@Transactional
class StudentAccessIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private StudentGuardianRepository guardianLinkRepository;

    @Autowired
    private StudentProfessionalLinkRepository professionalLinkRepository;

    @Autowired
    private ConsentTermRepository consentRepository;

    @Autowired
    private EntityManager entityManager;

    private User administrator;
    private User professional;
    private Student student;
    private Guardian guardian;
    private StudentGuardian guardianLink;
    private StudentProfessionalLink professionalLink;
    private RequestPostProcessor professionalToken;

    @BeforeEach
    void setUp() {
        administrator = userRepository.saveAndFlush(
                new User(
                        "Access Administrator",
                        "access-admin@conectatea.com",
                        "temporary-password-hash",
                        UserRole.ADMINISTRATOR,
                        TestCpf.next()
                )
        );

        professional = userRepository.saveAndFlush(
                new User(
                        "Maria Professora",
                        "access-teacher@conectatea.com",
                        "temporary-password-hash",
                        UserRole.TEACHER,
                        TestCpf.next()
                )
        );

        student = persistStudent("MAT-ACCESS-001");

        guardian = guardianRepository.saveAndFlush(
                new Guardian(
                        "Responsavel de Teste",
                        "52998224725",
                        "access-guardian@conectatea.com",
                        "47999999999",
                        null
                )
        );

        guardianLink = guardianLinkRepository.saveAndFlush(
                new StudentGuardian(
                        student,
                        guardian,
                        GuardianRelationship.MOTHER,
                        true,
                        true
                )
        );

        professionalLink = professionalLinkRepository.saveAndFlush(
                new StudentProfessionalLink(
                        student,
                        professional,
                        today().minusDays(1),
                        administrator.getId()
                )
        );

        professionalToken = jwt()
                .jwt(token -> token.subject(professional.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_TEACHER"));
    }

    @Test
    void directoryShouldListOnlyAuthorizedStudents() throws Exception {
        grantConsent();

        persistStudent("MAT-DIRECTORY-UNAUTHORIZED");

        readDirectory()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(student.getId().toString()))
                .andExpect(jsonPath("$.content[0].cpf").doesNotExist());
    }

    @Test
    void directoryShouldBeEmptyWithoutConsent() throws Exception {
        readDirectory()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void directoryShouldExcludeStudentsWithoutProfessionalLink() throws Exception {
        grantConsent();

        professionalLink.end(
                today(),
                administrator.getId(),
                "End of care"
        );
        professionalLinkRepository.saveAndFlush(professionalLink);

        readDirectory()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        readContacts().andExpect(status().isForbidden());
    }

    @Test
    void directoryShouldExcludeExpiredConsent() throws Exception {
        grantConsent(
                requiredPurposes(),
                Instant.now().minusSeconds(172800),
                today().minusDays(1)
        );

        readDirectory()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        readContacts().andExpect(status().isForbidden());
    }

    @Test
    void revokedConsentShouldBlockDirectoryAndContacts()
            throws Exception {
        ConsentTerm consent = grantConsent();

        consent.revoke(
                Instant.now(),
                administrator.getId(),
                "Guardian withdrew consent"
        );
        consentRepository.saveAndFlush(consent);

        readDirectory()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        readContacts().andExpect(status().isForbidden());
    }

    @Test
    void contactsShouldNotExposeCpfOrUserAccount() throws Exception {
        grantConsent();

        readContacts()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].guardianId").value(guardian.getId().toString()))
                .andExpect(jsonPath("$[0].fullName").value(guardian.getFullName()))
                .andExpect(jsonPath("$[0].phone").value(guardian.getPhone()))
                .andExpect(jsonPath("$[0].cpf").doesNotExist())
                .andExpect(jsonPath("$[0].userId").doesNotExist());
    }

    @Test
    void contactsShouldExcludeGuardianWithoutOwnValidConsent() throws Exception {
        grantConsent();

        Guardian anotherGuardian = guardianRepository.saveAndFlush(
                new Guardian(
                        "Another Legal Guardian",
                        TestCpf.next(),
                        "another-contact@example.com",
                        "47988887777",
                        null
                )
        );

        guardianLinkRepository.saveAndFlush(
                new StudentGuardian(
                        student,
                        anotherGuardian,
                        GuardianRelationship.FATHER,
                        true,
                        false
                )
        );

        readContacts()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].guardianId").value(guardian.getId().toString()));
    }

    @Test
    void inactiveGuardianShouldBlockDirectoryAndContacts() throws Exception {
        grantConsent();

        guardian.deactivate();
        guardianRepository.saveAndFlush(guardian);

        readDirectory()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        readContacts().andExpect(status().isForbidden());
    }

    @Test
    void inactiveStudentShouldDisappearFromDirectory() throws Exception {
        grantConsent();

        student.deactivate();
        studentRepository.saveAndFlush(student);

        readDirectory()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        readContacts().andExpect(status().isForbidden());
    }

    @Test
    void physicianShouldReadAuthorizedLegalGuardianContacts() throws Exception {
        User physician = userRepository.saveAndFlush(
                new User(
                        "Directory Physician",
                        "directory-physician@example.com",
                        "temporary-password-hash",
                        UserRole.PHYSICIAN,
                        TestCpf.next()
                )
        );

        professionalLinkRepository.saveAndFlush(
                new StudentProfessionalLink(
                        student,
                        physician,
                        today(),
                        administrator.getId()
                )
        );

        grantConsent(
                Set.of(
                        ConsentPurpose.MULTIPROFESSIONAL_MONITORING,
                        ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
                ),
                Instant.now().minusSeconds(60),
                today().plusYears(1)
        );

        RequestPostProcessor physicianToken = jwt()
                .jwt(token -> token.subject(physician.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_PHYSICIAN"));

        mockMvc.perform(get("/api/me/students").with(physicianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(
                        get("/api/me/students/{studentId}/legal-guardians", student.getId())
                                .with(physicianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guardianId").value(guardian.getId().toString()));
    }

    @Test
    void directoryShouldRejectInactiveProfessional() throws Exception {
        grantConsent();

        professional.deactivate();
        userRepository.saveAndFlush(professional);

        readDirectory().andExpect(status().isForbidden());

        readContacts().andExpect(status().isForbidden());
    }

    @Test
    void directoryShouldRejectInvalidPagination() throws Exception {
        mockMvc.perform(
                        get("/api/me/students")
                                .param("page", "-1")
                                .with(professionalToken)
                )
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        get("/api/me/students")
                                .param("size", "101")
                                .with(professionalToken)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void directoryAndContactsShouldRequireAuthentication()
            throws Exception {
        mockMvc.perform(get("/api/me/students"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                get("/api/me/students/{studentId}/legal-guardians", student.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administratorShouldNotHaveAutomaticProfessionalAccess() throws Exception {
        RequestPostProcessor administratorToken = jwt()
                .jwt(token -> token.subject(administrator.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"));

        mockMvc.perform(
                        get("/api/me/students")
                                .with(administratorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/me/students/{studentId}/legal-guardians", student.getId())
                                .with(administratorToken)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void authorizedTeacherShouldReadStudent() throws Exception {
        grantConsent();

        readProfile(student.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId().toString()))
                .andExpect(jsonPath("$.fullName").value("João Pedro da Silva"));
    }

    @Test
    void missingConsentShouldBeRejected() throws Exception {
        readProfile(student.getId()).andExpect(status().isForbidden());
    }

    @Test
    void missingSharingPurposeShouldBeRejected() throws Exception {
        grantConsent(
                Set.of(ConsentPurpose.EDUCATIONAL_SUPPORT),
                Instant.now().minusSeconds(60),
                today().plusYears(1)
        );

        readProfile(student.getId()).andExpect(status().isForbidden());
    }

    @Test
    void wrongProfessionalPurposeShouldBeRejected() throws Exception {
        grantConsent(
                Set.of(
                        ConsentPurpose.MULTIPROFESSIONAL_MONITORING,
                        ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
                ),
                Instant.now().minusSeconds(60),
                today().plusYears(1)
        );

        readProfile(student.getId()).andExpect(status().isForbidden());
    }

    @Test
    void revokedConsentShouldBlockSameToken() throws Exception {
        ConsentTerm consent = grantConsent();

        readProfile(student.getId()).andExpect(status().isOk());

        consent.revoke(
                Instant.now(),
                administrator.getId(),
                "Guardian withdrew consent"
        );
        consentRepository.saveAndFlush(consent);

        readProfile(student.getId()).andExpect(status().isForbidden());
    }

    @Test
    void endedLinkShouldBlockSameToken() throws Exception {
        grantConsent();

        readProfile(student.getId()).andExpect(status().isOk());

        professionalLink.end(
                today(),
                administrator.getId(),
                "Professional no longer provides care"
        );
        professionalLinkRepository.saveAndFlush(professionalLink);

        readProfile(student.getId()).andExpect(status().isForbidden());
    }

    @Test
    void expiredConsentShouldBeRejectedEvenWithActiveStatus() throws Exception {
        grantConsent(
                requiredPurposes(),
                Instant.now().minusSeconds(172800),
                today().minusDays(1)
        );

        readProfile(student.getId()).andExpect(status().isForbidden());
    }

    @Test
    void futureConsentShouldBeRejected() throws Exception {
        grantConsent(
                requiredPurposes(),
                Instant.now().plusSeconds(3600),
                today().plusYears(1)
        );

        readProfile(student.getId()).andExpect(status().isForbidden());
    }

    @Test
    void inactiveProfessionalShouldBeRejected() throws Exception {
        grantConsent();

        professional.deactivate();
        userRepository.saveAndFlush(professional);

        readProfile(student.getId()).andExpect(status().isForbidden());
    }

    @Test
    void inactiveStudentShouldBeRejected() throws Exception {
        grantConsent();

        student.deactivate();
        studentRepository.saveAndFlush(student);

        readProfile(student.getId()).andExpect(status().isForbidden());
    }

    @Test
    void inactiveGuardianShouldBeRejected() throws Exception {
        grantConsent();

        guardian.deactivate();
        guardianRepository.saveAndFlush(guardian);

        readProfile(student.getId()).andExpect(status().isForbidden());
    }

    @Test
    void nonLegalGuardianShouldNotAuthorizeAccess() throws Exception {
        grantConsent();

        guardianLink.update(GuardianRelationship.MOTHER, false, true);
        guardianLinkRepository.saveAndFlush(guardianLink);

        readProfile(student.getId()).andExpect(status().isForbidden());
    }

    @Test
    void consentForOneStudentShouldNotAuthorizeAnother() throws Exception {
        grantConsent();

        Student anotherStudent = persistStudent("MAT-ACCESS-002");

        professionalLinkRepository.saveAndFlush(
                new StudentProfessionalLink(
                        anotherStudent,
                        professional,
                        today(),
                        administrator.getId()
                )
        );

        readProfile(anotherStudent.getId()).andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(
                get("/api/me/students/{studentId}", student.getId()))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions readDirectory() throws Exception {
        entityManager.flush();
        entityManager.clear();

        return mockMvc.perform(get("/api/me/students").with(professionalToken));
    }

    private ResultActions readContacts() throws Exception {
        entityManager.flush();
        entityManager.clear();

        return mockMvc.perform(
                get("/api/me/students/{studentId}/legal-guardians",
                        student.getId()
                ).with(professionalToken)
        );
    }

    private ResultActions readProfile(UUID studentId) throws Exception {
        entityManager.flush();
        entityManager.clear();

        return mockMvc.perform(get("/api/me/students/{studentId}", studentId).with(professionalToken));
    }

    private ConsentTerm grantConsent() {
        return grantConsent(
                requiredPurposes(),
                Instant.now().minusSeconds(60),
                today().plusYears(1)
        );
    }

    private ConsentTerm grantConsent(
            Set<ConsentPurpose> purposes,
            Instant grantedAt,
            LocalDate validUntil
    ) {
        return consentRepository.saveAndFlush(
                new ConsentTerm(
                        guardianLink,
                        purposes,
                        "1.0",
                        grantedAt,
                        validUntil,
                        administrator.getId()
                )
        );
    }

    private Student persistStudent(String enrollmentNumber) {
        return studentRepository.saveAndFlush(
                new Student(
                        "João Pedro da Silva",
                        "João",
                        LocalDate.of(2015, 5, 10),
                        enrollmentNumber,
                        2026,
                        "5º ano",
                        "Turma B",
                        TestCpf.next()
                )
        );
    }

    private static Set<ConsentPurpose> requiredPurposes() {
        return Set.of(
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
        );
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}