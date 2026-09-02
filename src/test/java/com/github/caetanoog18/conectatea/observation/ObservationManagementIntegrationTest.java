package com.github.caetanoog18.conectatea.observation;

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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@AutoConfigureMockMvc
@Transactional
class ObservationManagementIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private GuardianRepository guardianRepository;
    @Autowired private StudentGuardianRepository guardianLinkRepository;
    @Autowired private StudentProfessionalLinkRepository professionalLinkRepository;
    @Autowired private ConsentTermRepository consentRepository;
    @Autowired private ObservationRepository observationRepository;
    @Autowired private EntityManager entityManager;

    private User administrator;
    private User teacher;
    private Student student;
    private StudentGuardian guardianLink;
    private StudentProfessionalLink professionalLink;

    @BeforeEach
    void setUp() {
        administrator = persistUser(
                "observation-admin@example.com",
                UserRole.ADMINISTRATOR
        );

        teacher = persistUser(
                "observation-teacher@example.com",
                UserRole.TEACHER
        );

        student = persistStudent("OBS-API-001");

        Guardian guardian = guardianRepository.saveAndFlush(
                new Guardian(
                        "Responsavel de Teste",
                        null,
                        "observation-guardian@example.com",
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

        professionalLink = professionalLinkRepository.saveAndFlush(
                new StudentProfessionalLink(student, teacher, today().minusDays(1), administrator.getId())
        );
    }

    @Test
    void authorizedTeacherShouldCreateObservation() throws Exception {
        grantConsent();

        createAs(teacher)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.studentId").value(student.getId().toString()))
                .andExpect(jsonPath("$.authorId").value(teacher.getId().toString()))
                .andExpect(jsonPath("$.purpose").value("EDUCATIONAL_SUPPORT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        var page = observationRepository
                .findAllByStudent_IdAndPurposeOrderByOccurredAtDescIdDesc(
                        student.getId(),
                        ConsentPurpose.EDUCATIONAL_SUPPORT,
                        org.springframework.data.domain.PageRequest.of(0, 20)
                );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getAuthor().getId())
                .isEqualTo(teacher.getId());
    }

    @Test
    void missingConsentShouldBlockReadAndCreate() throws Exception {
        long countBefore = observationRepository.count();

        createAs(teacher).andExpect(status().isForbidden());
        listAsTeacher().andExpect(status().isForbidden());

        assertThat(observationRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void missingSharingPurposeShouldBlockReadAndCreate() throws Exception {
        grantConsent(Set.of(ConsentPurpose.EDUCATIONAL_SUPPORT));

        createAs(teacher).andExpect(status().isForbidden());
        listAsTeacher().andExpect(status().isForbidden());
    }

    @Test
    void administratorShouldNotBypassProfessionalAuthorization() throws Exception {
        grantConsent();

        createAs(administrator).andExpect(status().isForbidden());
    }

    @Test
    void tokenRoleShouldNotOverrideDatabaseRole() throws Exception {
        grantConsent();

        User guardianUser = persistUser(
                "observation-guardian-user@example.com",
                UserRole.LEGAL_GUARDIAN
        );
        createAs(guardianUser).andExpect(status().isForbidden());
    }

    @Test
    void inactiveTeacherShouldBeRejected() throws Exception {
        grantConsent();

        teacher.deactivate();
        userRepository.saveAndFlush(teacher);

        createAs(teacher).andExpect(status().isForbidden());
        listAsTeacher().andExpect(status().isForbidden());
    }

    @Test
    void endedLinkShouldBlockNewRequestsWithSameIdentity() throws Exception {
        grantConsent();
        createAs(teacher).andExpect(status().isCreated());

        professionalLink.end(
                today(),
                administrator.getId(),
                "End of professional assignment"
        );
        professionalLinkRepository.saveAndFlush(professionalLink);

        listAsTeacher().andExpect(status().isForbidden());
        createAs(teacher).andExpect(status().isForbidden());
    }

    @Test
    void revokedConsentShouldBlockNewRequestsWithSameIdentity() throws Exception {
        ConsentTerm consent = grantConsent();
        createAs(teacher).andExpect(status().isCreated());

        consent.revoke(
                Instant.now(),
                administrator.getId(),
                "Consent withdrawn"
        );
        consentRepository.saveAndFlush(consent);

        listAsTeacher().andExpect(status().isForbidden());
        createAs(teacher).andExpect(status().isForbidden());
    }

    @Test
    void queriesShouldNotExposeAnotherStudentOrPurpose() throws Exception {
        grantConsent();

        Observation educational = persistObservation(student, teacher, ConsentPurpose.EDUCATIONAL_SUPPORT);

        User psychologist = persistUser("observation-psychologist@example.com", UserRole.PSYCHOLOGIST);

        Observation multiprofessional = persistObservation(
                student, psychologist, ConsentPurpose.MULTIPROFESSIONAL_MONITORING);

        Student anotherStudent = persistStudent("OBS-API-002");

        Observation unrelated = persistObservation(anotherStudent, teacher, ConsentPurpose.EDUCATIONAL_SUPPORT);

        listAsTeacher()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(educational.getId().toString()));

        findAsTeacher(educational.getId()).andExpect(status().isOk());

        findAsTeacher(multiprofessional.getId()).andExpect(status().isNotFound());

        findAsTeacher(unrelated.getId()).andExpect(status().isNotFound());
    }

    @Test
    void invalidBodyShouldBeRejected() throws Exception {
        grantConsent();

        mockMvc.perform(
                        post(baseUrl())
                                .with(jwt().jwt(token ->
                                        token.subject(teacher.getEmail())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "",
                                          "content": "",
                                          "occurredAt": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidPaginationShouldBeRejected() throws Exception {
        grantConsent();

        mockMvc.perform(
                        get(baseUrl())
                                .param("size", "101")
                                .with(jwt().jwt(token -> token.subject(teacher.getEmail()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousRequestShouldBeRejected() throws Exception {
        mockMvc.perform(
                        post(baseUrl())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody())
                )
                .andExpect(status().isUnauthorized());
    }

    private ResultActions createAs(User user) throws Exception {
        flushAndClear();

        return mockMvc.perform(
                post(baseUrl())
                        .with(jwt()
                                .jwt(token -> token.subject(user.getEmail()))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_TEACHER")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody())
        );
    }

    private ResultActions listAsTeacher() throws Exception {
        flushAndClear();

        return mockMvc.perform(
                get(baseUrl()).with(jwt().jwt(token -> token.subject(teacher.getEmail())))
        );
    }

    private ResultActions findAsTeacher(UUID observationId) throws Exception {
        flushAndClear();

        return mockMvc.perform(
                get(baseUrl() + "/" + observationId)
                        .with(jwt().jwt(token -> token.subject(teacher.getEmail())))
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private String baseUrl() {
        return "/api/me/students/" + student.getId() + "/observations";
    }

    private static String requestBody() {
        return """
                {
                  "title": "Participacao em atividade",
                  "content": "Participou da atividade com apoio visual.",
                  "occurredAt": "2026-01-10T14:00:00Z"
                }
                """;
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
                new User(
                        "Usuario de Teste",
                        email,
                        "temporary-password-hash",
                        role
                )
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

    private Observation persistObservation(
            Student targetStudent,
            User author,
            ConsentPurpose purpose
    ) {
        return observationRepository.saveAndFlush(
                new Observation(
                        targetStudent,
                        author,
                        purpose,
                        "Registro Teste",
                        "Conteudo ficticio para teste.",
                        Instant.parse("2026-01-10T14:00:00Z")
                )
        );
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}