package com.github.caetanoog18.conectatea.timeline;

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
import com.github.caetanoog18.conectatea.observation.domain.Observation;
import com.github.caetanoog18.conectatea.observation.infrastructure.ObservationRepository;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class TimelineIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private GuardianRepository guardianRepository;
    @Autowired private StudentGuardianRepository guardianLinkRepository;
    @Autowired private StudentProfessionalLinkRepository linkRepository;
    @Autowired private ConsentTermRepository consentRepository;
    @Autowired private ObservationRepository observationRepository;

    private User administrator;
    private User teacher;
    private Student student;
    private StudentGuardian guardianLink;
    private StudentProfessionalLink professionalLink;

    @BeforeEach
    void setUp() {
        administrator = persistUser(
                "timeline-admin@example.com",
                UserRole.ADMINISTRATOR
        );

        teacher = persistUser(
                "timeline-teacher@example.com",
                UserRole.TEACHER
        );

        student = persistStudent("TIMELINE-001");

        Guardian guardian = guardianRepository.saveAndFlush(
                new Guardian(
                        "Responsavel de Teste",
                        null,
                        "timeline-guardian@example.com",
                        "00000000000",
                        null
                )
        );

        guardianLink = guardianLinkRepository.saveAndFlush(
                new StudentGuardian(
                        student,
                        guardian,
                        GuardianRelationship.LEGAL_GUARDIAN,
                        true,
                        true
                )
        );

        professionalLink = linkRepository.saveAndFlush(
                new StudentProfessionalLink(student, teacher, today().minusDays(1), administrator.getId()));
    }

    @Test
    void authorizedStudentWithoutObservationsShouldReturnEmptyPage() throws Exception {
        grantConsent();

        read()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldOrderAndPaginateWithDeterministicTieBreak() throws Exception {
        grantConsent();

        Observation older = educational("Antiga", "2026-01-10T14:00:00Z");
        Observation first = educational("Primeira com mesmo horario", "2026-01-11T14:00:00Z");
        Observation second = educational("Segunda com mesmo horario", "2026-01-11T14:00:00Z");

        List<String> orderedIds = List.of(first.getId().toString(), second.getId().toString())
                .stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        read("page", "0", "size", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(orderedIds.get(0)))
                .andExpect(jsonPath("$.content[1].id").value(orderedIds.get(1)))
                .andExpect(jsonPath("$.content[0].type").value("OBSERVATION"));

        read("page", "1", "size", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(older.getId().toString()));
    }

    @Test
    void shouldUseInclusiveFromAndExclusiveTo() throws Exception {
        grantConsent();

        educational("Antes", "2026-01-09T23:59:59Z");
        educational("Inicio", "2026-01-10T00:00:00Z");
        educational("Dentro", "2026-01-10T12:00:00Z");
        educational("Limite final", "2026-01-11T00:00:00Z");

        read("from", "2026-01-10T00:00:00Z", "to", "2026-01-11T00:00:00Z")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Dentro"))
                .andExpect(jsonPath("$.content[1].title").value("Inicio"));

        read("from", "2026-01-10T00:00:00Z")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        read("to", "2026-01-10T00:00:00Z")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldFilterStudentAndPurposeIncludingTotalCount() throws Exception {
        grantConsent();

        Observation allowed = educational("Registro autorizado", "2026-01-10T14:00:00Z");

        User psychologist = persistUser("timeline-psychologist@example.com", UserRole.PSYCHOLOGIST);

        persistObservation(
                student,
                psychologist,
                ConsentPurpose.MULTIPROFESSIONAL_MONITORING,
                "Registro multiprofissional",
                "2026-01-11T14:00:00Z"
        );

        Student anotherStudent = persistStudent("TIMELINE-002");

        persistObservation(anotherStudent, teacher,
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                "Outro estudante",
                "2026-01-12T14:00:00Z"
        );

        read("purpose", "MULTIPROFESSIONAL_MONITORING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(allowed.getId().toString()))
                .andExpect(jsonPath("$.content[0].purpose").value("EDUCATIONAL_SUPPORT"));
    }

    @Test
    void missingConsentShouldBeRejected() throws Exception {
        read().andExpect(status().isForbidden());
    }

    @Test
    void wrongConsentPurposeShouldBeRejected() throws Exception {
        grantConsent(Set.of(
                ConsentPurpose.MULTIPROFESSIONAL_MONITORING,
                ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
        ));

        read().andExpect(status().isForbidden());
    }

    @Test
    void endedLinkShouldBlockNextRequest() throws Exception {
        grantConsent();

        read().andExpect(status().isOk());

        professionalLink.end(today(), administrator.getId(), "Assignment ended");
        linkRepository.saveAndFlush(professionalLink);

        read().andExpect(status().isForbidden());
    }

    @Test
    void revokedConsentShouldBlockNextRequest() throws Exception {
        ConsentTerm consent = grantConsent();

        read().andExpect(status().isOk());

        consent.revoke(Instant.now(), administrator.getId(), "Consent withdrawn");
        consentRepository.saveAndFlush(consent);

        read().andExpect(status().isForbidden());
    }

    @Test
    void inactiveTeacherShouldBeRejected() throws Exception {
        grantConsent();

        teacher.deactivate();
        userRepository.saveAndFlush(teacher);

        read().andExpect(status().isForbidden());
    }

    @Test
    void administratorShouldNotGainAccessWithTeacherAuthority() throws Exception {
        grantConsent();

        mockMvc.perform(
                        get(baseUrl())
                                .with(jwt()
                                        .jwt(token -> token.subject(administrator.getEmail()))
                                        .authorities(new SimpleGrantedAuthority("ROLE_TEACHER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidFiltersShouldBeRejected() throws Exception {
        grantConsent();

        read("from", "2026-01-11T00:00:00Z", "to", "2026-01-10T00:00:00Z")
                .andExpect(status().isBadRequest());

        read("from", "2026-01-10T00:00:00Z", "to", "2026-01-10T00:00:00Z")
                .andExpect(status().isBadRequest());

        read("from", "invalid-date")
                .andExpect(status().isBadRequest());

        read("page", "-1").andExpect(status().isBadRequest());
        read("size", "0").andExpect(status().isBadRequest());
        read("size", "101").andExpect(status().isBadRequest());
    }

    @Test
    void anonymousRequestShouldBeRejected() throws Exception {
        mockMvc.perform(get(baseUrl())).andExpect(status().isUnauthorized());
    }

    private ResultActions read(String... parameters) throws Exception {
        entityManager.flush();
        entityManager.clear();

        var request = get(baseUrl()).with(jwt().jwt(token -> token.subject(teacher.getEmail())));

        for (int index = 0; index < parameters.length; index += 2) {
            request.param(parameters[index], parameters[index + 1]);
        }

        return mockMvc.perform(request);
    }

    private String baseUrl() {
        return "/api/me/students/" + student.getId() + "/timeline";
    }

    private ConsentTerm grantConsent() {
        return grantConsent(Set.of(
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
        ));
    }

    private ConsentTerm grantConsent(Set<ConsentPurpose> purposes) {
        return consentRepository.saveAndFlush(
                new ConsentTerm(
                        guardianLink,
                        purposes,
                        "test-1.0",
                        Instant.now().minusSeconds(60),
                        today().plusDays(30),
                        administrator.getId()
                )
        );
    }

    private User persistUser(String email, UserRole role) {
        return userRepository.saveAndFlush(
                new User("Usuario de Teste", email, "temporary-password-hash", role)
        );
    }

    private Student persistStudent(String enrollmentNumber) {
        return studentRepository.saveAndFlush(
                new Student(
                        "Estudante de Teste",
                        null,
                        LocalDate.of(2015, 5, 10),
                        enrollmentNumber,
                        2026,
                        "5 ano",
                        "Turma Teste"
                )
        );
    }

    private Observation educational(String title, String occurredAt) {
        return persistObservation(
                student,
                teacher,
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                title,
                occurredAt
        );
    }

    private Observation persistObservation(
            Student targetStudent,
            User author,
            ConsentPurpose purpose,
            String title,
            String occurredAt
    ) {
        return observationRepository.saveAndFlush(
                new Observation(
                        targetStudent,
                        author,
                        purpose,
                        title,
                        "Conteudo ficticio para teste da linha do tempo.",
                        Instant.parse(occurredAt)
                )
        );
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}