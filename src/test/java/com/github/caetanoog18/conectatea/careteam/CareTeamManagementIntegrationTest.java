package com.github.caetanoog18.conectatea.careteam;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.careteam.domain.StudentProfessionalLink;
import com.github.caetanoog18.conectatea.careteam.infrastructure.StudentProfessionalLinkRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

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
class CareTeamManagementIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentProfessionalLinkRepository linkRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User administrator;
    private User professional;
    private Student student;

    @BeforeEach
    void setUp() {
        administrator = persistUser(
                "Care Team Administrator",
                "careteam-admin@conectatea.com",
                UserRole.ADMINISTRATOR
        );

        professional = persistUser(
                "Ana Psicóloga",
                "ana.psicologa@conectatea.com",
                UserRole.PSYCHOLOGIST
        );

        student = studentRepository.saveAndFlush(
                new Student(
                        "João Pedro da Silva",
                        "João",
                        LocalDate.of(2015, 5, 10),
                        "MAT-CARETEAM-001",
                        2026,
                        "5º ano",
                        "Turma B"
                )
        );
    }

    @Test
    void administratorShouldCreateLink() throws Exception {
        createLink(administrator, professional.getId(), today())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.studentId").value(student.getId().toString()))
                .andExpect(jsonPath("$.professionalId").value(professional.getId().toString()))
                .andExpect(jsonPath("$.professionalRole").value("PSYCHOLOGIST"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdByUserId").value(administrator.getId().toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertThat(
                linkRepository
                        .existsByStudent_IdAndProfessional_IdAndActiveTrue(student.getId(), professional.getId()))
                .isTrue();
    }

    @Test
    void coordinatorShouldCreateLink() throws Exception {
        User coordinator = persistUser(
                "Pedagogical Coordinator",
                "coordinator@conectatea.com",
                UserRole.PEDAGOGICAL_COORDINATOR
        );

        createLink(coordinator, professional.getId(), today())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdByUserId").value(coordinator.getId().toString()));
    }

    @Test
    void duplicateActiveLinkShouldBeRejected() throws Exception {
        persistLink();

        createLink(administrator, professional.getId(), today())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Care team conflict"));

        assertThat(linkRepository.count()).isEqualTo(1);
    }

    @Test
    void administratorShouldNotBeAssignedAsProfessional() throws Exception {
        createLink(administrator, administrator.getId(), today())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail")
                        .value("User role is not eligible for the care team"));

        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void inactiveProfessionalShouldBeRejected() throws Exception {
        professional.deactivate();
        userRepository.saveAndFlush(professional);

        createLink(administrator, professional.getId(), today())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Professional must be active"));

        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void inactiveStudentShouldBeRejected() throws Exception {
        student.deactivate();
        studentRepository.saveAndFlush(student);

        createLink(administrator, professional.getId(), today())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Student must be active"));

        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void futureStartDateShouldBeRejected() throws Exception {
        createLink(
                administrator,
                professional.getId(),
                today().plusDays(1))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Start date cannot be in the future"));

        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void inactiveManagerShouldBeRejectedEvenWithAdminToken() throws Exception {
        administrator.deactivate();
        userRepository.saveAndFlush(administrator);

        createLink(administrator, professional.getId(), today()).andExpect(status().isForbidden());
        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void tokenAuthorityShouldNotOverrideCurrentDatabaseRole() throws Exception {
        mockMvc.perform(
                        post("/api/students/{studentId}/care-team", student.getId())
                                .with(jwt()
                                        .jwt(token -> token.subject(
                                                professional.getEmail()))
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")
                                        ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequest(professional.getId(), today()))
                )
                .andExpect(status().isForbidden());

        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void administratorShouldListAndFindLink() throws Exception {
        StudentProfessionalLink link = persistLink();

        mockMvc.perform(
                        get("/api/students/{studentId}/care-team", student.getId())
                                .with(asUser(administrator))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(link.getId().toString()))
                .andExpect(jsonPath("$[0].professionalName").value("Ana Psicóloga"));

        mockMvc.perform(
                get("/api/care-team-links/{linkId}", link.getId()).with(asUser(administrator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(link.getId().toString()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldEndLinkAndPreserveHistoryWhenProfessionalReturns() throws Exception {
        StudentProfessionalLink link = persistLink();UUID previousLinkId = link.getId();
        endLink(previousLinkId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.endedOn")
                        .value(today().toString()))
                .andExpect(jsonPath("$.endedByUserId")
                        .value(administrator.getId().toString()))
                .andExpect(jsonPath("$.endReason")
                        .value("Professional no longer provides care"));

        entityManager.flush();
        entityManager.clear();

        StudentProfessionalLink endedLink = linkRepository.findById(previousLinkId).orElseThrow();

        assertThat(endedLink.isActive()).isFalse();

        assertThat(
                linkRepository.existsByStudent_IdAndProfessional_IdAndActiveTrue(
                        student.getId(), professional.getId())).isFalse();

        createLink(administrator, professional.getId(), today())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true));

        assertThat(linkRepository.count()).isEqualTo(2);

        assertThat(linkRepository.findById(previousLinkId).orElseThrow().isActive()).isFalse();

        mockMvc.perform(
                        get("/api/students/{studentId}/care-team", student.getId())
                                .with(asUser(administrator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void alreadyEndedLinkShouldBeRejected() throws Exception {
        StudentProfessionalLink link = persistLink();

        link.end(today(), administrator.getId(), "Original end reason");

        linkRepository.saveAndFlush(link);

        endLink(link.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Professional link is already inactive"));

        assertThat(linkRepository.findById(link.getId())
                        .orElseThrow()
                        .getEndReason())
                .isEqualTo("Original end reason");
    }

    @Test
    void incompleteRequestsShouldBeRejected() throws Exception {
        mockMvc.perform(
                post("/api/students/{studentId}/care-team", student.getId())
                                .with(asUser(administrator))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(linkRepository.count()).isZero();

        StudentProfessionalLink link = persistLink();

        mockMvc.perform(
                        patch("/api/care-team-links/{linkId}/end", link.getId())
                                .with(asUser(administrator))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"reason": " "}
                                        """)
                )
                .andExpect(status().isBadRequest());

        assertThat(linkRepository.findById(link.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void missingLinkShouldReturnNotFound() throws Exception {
        mockMvc.perform(
                        get("/api/care-team-links/{linkId}", UUID.randomUUID()).with(asUser(administrator)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title")
                        .value("Care team resource not found"));
    }

    @Test
    void teacherShouldNotManageCareTeam() throws Exception {
        User teacher = persistUser("Maria Professora", "teacher-careteam@conectatea.com",
                UserRole.TEACHER);

        createLink(teacher, professional.getId(), today()).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/students/{studentId}/care-team", student.getId())
                .with(asUser(teacher))).andExpect(status().isForbidden());

        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(
                        get("/api/students/{studentId}/care-team", student.getId()))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions createLink(User manager, UUID professionalId, LocalDate startedOn) throws Exception {
        return mockMvc.perform(
                post("/api/students/{studentId}/care-team", student.getId())
                        .with(asUser(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(
                                professionalId,
                                startedOn
                        ))
        );
    }

    private ResultActions endLink(UUID linkId) throws Exception {
        return mockMvc.perform(
                patch("/api/care-team-links/{linkId}/end", linkId)
                        .with(asUser(administrator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Professional no longer provides care"
                                }
                                """)
        );
    }

    private StudentProfessionalLink persistLink() {
        return linkRepository.saveAndFlush(
                new StudentProfessionalLink(student, professional, today().minusDays(1), administrator.getId())
        );
    }

    private User persistUser(String fullName, String email, UserRole role) {
        return userRepository.saveAndFlush(new User(fullName, email, "temporary-password-hash", role));
    }

    private static String createRequest(UUID professionalId, LocalDate startedOn) {
        return """
                {
                  "professionalId": "%s",
                  "startedOn": "%s"
                }
                """.formatted(professionalId, startedOn);
    }

    private static RequestPostProcessor asUser(User user) {
        return jwt().jwt(token -> token.subject(user.getEmail())).authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        )
                );
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}