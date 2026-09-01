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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@AutoConfigureMockMvc
@Transactional
class ConsentManagementIntegrationTest {
    private static final String ADMIN_EMAIL = "consent-admin@conectatea.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConsentTermRepository consentRepository;

    @Autowired
    private StudentGuardianRepository linkRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void administratorShouldCreateConsent() throws Exception {
        TestContext context = persistContext(true);

        mockMvc.perform(
                        post("/api/student-guardian-links/{linkId}/consents", context.link().getId())
                                .with(withRole("ADMINISTRATOR", ADMIN_EMAIL))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createConsentRequest())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.studentGuardianId").value(context.link().getId().toString()))
                .andExpect(jsonPath("$.studentId").value(context.student().getId().toString()))
                .andExpect(jsonPath("$.guardianId").value(context.guardian().getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.termsVersion").value("1.0"))
                .andExpect(jsonPath("$.purposes.length()").value(2));

        assertThat(consentRepository.count()).isEqualTo(1);
    }

    @Test
    void duplicateActiveConsentShouldBeRejected() throws Exception {
        TestContext context = persistContext(true);
        persistActiveConsent(context);

        mockMvc.perform(
                        post("/api/student-guardian-links/{linkId}/consents", context.link().getId())
                                .with(withRole(
                                        "ADMINISTRATOR",
                                        ADMIN_EMAIL
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createConsentRequest())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Consent conflict"))
                .andExpect(jsonPath("$.detail")
                        .value("Student guardian link already has " + "an active consent"));

        assertThat(consentRepository.count()).isEqualTo(1);
    }

    @Test
    void nonLegalGuardianShouldBeRejected() throws Exception {
        TestContext context = persistContext(false);

        mockMvc.perform(
                        post("/api/student-guardian-links/{linkId}/consents", context.link().getId())
                                .with(withRole("ADMINISTRATOR", ADMIN_EMAIL))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createConsentRequest())
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid consent"))
                .andExpect(jsonPath("$.detail").value("Consent can only be registered " + "for a legal guardian"));

        assertThat(consentRepository.count()).isZero();
    }

    @Test
    void shouldReturnActiveConsentAndHistory() throws Exception {
        TestContext context = persistContext(true);

        ConsentTerm previousConsent = new ConsentTerm(
                context.link(),
                Set.of(ConsentPurpose.EDUCATIONAL_SUPPORT),
                "0.9",
                Instant.now().minusSeconds(172800),
                currentDate().plusMonths(6),
                context.administrator().getId()
        );

        previousConsent.revoke(
                Instant.now().minusSeconds(86400),
                context.administrator().getId(),
                "Term replaced by a newer version"
        );

        consentRepository.saveAndFlush(previousConsent);

        ConsentTerm activeConsent = persistActiveConsent(context);

        mockMvc.perform(
                        get("/api/student-guardian-links/{linkId}" + "/consents/active", context.link().getId())
                                .with(withRole("ADMINISTRATOR", ADMIN_EMAIL))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activeConsent.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(
                        get("/api/student-guardian-links/{linkId}" + "/consents", context.link().getId())
                                .with(withRole(
                                        "ADMINISTRATOR",
                                        ADMIN_EMAIL
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].status").value("REVOKED"));
    }

    @Test
    void expiredConsentShouldAllowNewConsent() throws Exception {
        TestContext context = persistContext(true);

        ConsentTerm expiredConsent = new ConsentTerm(
                context.link(),
                Set.of(ConsentPurpose.EDUCATIONAL_SUPPORT),
                "0.9",
                Instant.now().minusSeconds(172800),
                currentDate().minusDays(1),
                context.administrator().getId()
        );

        consentRepository.saveAndFlush(expiredConsent);

        mockMvc.perform(
                        post("/api/student-guardian-links/{linkId}/consents", context.link().getId())
                                .with(withRole(
                                        "ADMINISTRATOR",
                                        ADMIN_EMAIL
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createConsentRequest())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        ConsentTerm updatedPreviousConsent = consentRepository.findById(expiredConsent.getId()).orElseThrow();

        assertThat(updatedPreviousConsent.getStatus()).isEqualTo(ConsentStatus.EXPIRED);
        assertThat(consentRepository.count()).isEqualTo(2);
    }

    @Test
    void administratorShouldRevokeConsent() throws Exception {
        TestContext context = persistContext(true);
        ConsentTerm consent = persistActiveConsent(context);

        mockMvc.perform(
                        patch("/api/consents/{consentId}/revoke", consent.getId())
                                .with(withRole(
                                        "ADMINISTRATOR",
                                        ADMIN_EMAIL
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "reason": "Guardian withdrew consent"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.revokedAt").isNotEmpty())
                .andExpect(jsonPath("$.revokedByUserId")
                        .value(
                                context.administrator()
                                        .getId()
                                        .toString()
                        ))
                .andExpect(jsonPath("$.revocationReason")
                        .value("Guardian withdrew consent"));

        ConsentTerm revokedConsent = consentRepository.findById(consent.getId()).orElseThrow();

        assertThat(revokedConsent.getStatus()).isEqualTo(ConsentStatus.REVOKED);
        assertThat(revokedConsent.getRevokedAt()).isNotNull();
    }

    @Test
    void teacherShouldNotCreateConsent() throws Exception {
        TestContext context = persistContext(true);

        mockMvc.perform(
                        post("/api/student-guardian-links/{linkId}/consents", context.link().getId())
                                .with(withRole(
                                        "TEACHER",
                                        "teacher@conectatea.com"
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createConsentRequest())
                )
                .andExpect(status().isForbidden());

        assertThat(consentRepository.count()).isZero();
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        TestContext context = persistContext(true);

        mockMvc.perform(
                        get("/api/student-guardian-links/{linkId}" + "/consents",
                                context.link().getId()
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    private TestContext persistContext(boolean legalGuardian) {
        User administrator = userRepository.saveAndFlush(
                new User(
                        "Consent Administrator",
                        ADMIN_EMAIL,
                        "temporary-password-hash",
                        UserRole.ADMINISTRATOR
                )
        );

        Student student = studentRepository.saveAndFlush(
                new Student(
                        "João Pedro da Silva",
                        "João",
                        LocalDate.of(2015, 5, 10),
                        "MAT-CONSENT-001",
                        2026,
                        "5º ano",
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

        StudentGuardian link = linkRepository.saveAndFlush(
                new StudentGuardian(
                        student,
                        guardian,
                        GuardianRelationship.MOTHER,
                        legalGuardian,
                        true
                )
        );

        return new TestContext(administrator, student, guardian, link);
    }

    private ConsentTerm persistActiveConsent(TestContext context) {
        return consentRepository.saveAndFlush(
                new ConsentTerm(
                        context.link(),
                        Set.of(
                                ConsentPurpose.EDUCATIONAL_SUPPORT,
                                ConsentPurpose.MULTIPROFESSIONAL_MONITORING
                        ),
                        "1.0",
                        Instant.now().minusSeconds(60),
                        currentDate().plusYears(1),
                        context.administrator().getId()
                )
        );
    }

    private static String createConsentRequest() {
        return """
                {
                  "purposes": [
                    "EDUCATIONAL_SUPPORT",
                    "MULTIPROFESSIONAL_MONITORING"
                  ],
                  "termsVersion": "1.0",
                  "grantedAt": "%s",
                  "validUntil": "%s"
                }
                """.formatted(
                Instant.now().minusSeconds(60),
                currentDate().plusYears(1)
        );
    }

    private static RequestPostProcessor withRole(String role, String subject) {
        return jwt()
                .jwt(jwt -> jwt.subject(subject))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );
    }

    private static LocalDate currentDate() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    private record TestContext(User administrator, Student student, Guardian guardian, StudentGuardian link) { }
}