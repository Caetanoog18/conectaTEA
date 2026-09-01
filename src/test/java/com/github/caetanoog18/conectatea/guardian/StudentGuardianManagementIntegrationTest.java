package com.github.caetanoog18.conectatea.guardian;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.guardian.domain.Guardian;
import com.github.caetanoog18.conectatea.guardian.domain.GuardianRelationship;
import com.github.caetanoog18.conectatea.guardian.domain.StudentGuardian;
import com.github.caetanoog18.conectatea.guardian.infrastructure.GuardianRepository;
import com.github.caetanoog18.conectatea.guardian.infrastructure.StudentGuardianRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class StudentGuardianManagementIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private StudentGuardianRepository linkRepository;

    @Test
    void administratorShouldCreateLink() throws Exception {
        Student student = persistStudent();
        Guardian guardian = persistGuardian("Maria da Silva", "52998224725");

        mockMvc.perform(
                        post("/api/students/{studentId}/guardians", student.getId())
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createLinkRequest(
                                        guardian.getId(),
                                        "MOTHER",
                                        true,
                                        true
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.studentId")
                        .value(student.getId().toString()))
                .andExpect(jsonPath("$.guardianId")
                        .value(guardian.getId().toString()))
                .andExpect(jsonPath("$.relationship")
                        .value("MOTHER"))
                .andExpect(jsonPath("$.legalGuardian")
                        .value(true))
                .andExpect(jsonPath("$.primaryContact")
                        .value(true));

        assertThat(linkRepository.count()).isEqualTo(1);
    }

    @Test
    void coordinatorShouldCreateLink() throws Exception {
        Student student = persistStudent();
        Guardian guardian = persistGuardian("Maria da Silva", "52998224725");
        mockMvc.perform(
                        post("/api/students/{studentId}/guardians", student.getId())
                                .with(withRole(
                                        "PEDAGOGICAL_COORDINATOR"
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createLinkRequest(
                                        guardian.getId(),
                                        "MOTHER",
                                        true,
                                        true
                                ))
                )
                .andExpect(status().isCreated());
    }

    @Test
    void teacherShouldNotCreateLink() throws Exception {
        Student student = persistStudent();
        Guardian guardian = persistGuardian("Maria da Silva", "52998224725");
        mockMvc.perform(
                        post("/api/students/{studentId}/guardians", student.getId())
                                .with(withRole("TEACHER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createLinkRequest(
                                        guardian.getId(),
                                        "MOTHER",
                                        true,
                                        true
                                ))
                )
                .andExpect(status().isForbidden());

        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void shouldListLinksByStudentAndGuardian() throws Exception {
        Student student = persistStudent();
        Guardian guardian = persistGuardian("Maria da Silva", "52998224725");

        persistLink(student, guardian, true);

        mockMvc.perform(
                        get("/api/students/{studentId}/guardians", student.getId())
                                .with(withRole("ADMINISTRATOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].guardianName").value("Maria da Silva"));

        mockMvc.perform(
                        get("/api/guardians/{guardianId}/students", guardian.getId())
                                .with(withRole("ADMINISTRATOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentName").value("João da Silva"));
    }

    @Test
    void administratorShouldUpdateLink() throws Exception {
        Student student = persistStudent();
        Guardian guardian = persistGuardian("Maria da Silva", "52998224725");

        persistLink(student, guardian, true);

        String requestBody = """
                {
                  "relationship": "LEGAL_GUARDIAN",
                  "legalGuardian": true,
                  "primaryContact": false
                }
                """;

        mockMvc.perform(
                        put("/api/students/{studentId}/guardians/{guardianId}", student.getId(), guardian.getId())
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relationship")
                        .value("LEGAL_GUARDIAN"))
                .andExpect(jsonPath("$.legalGuardian")
                        .value(true))
                .andExpect(jsonPath("$.primaryContact")
                        .value(false));

        StudentGuardian updated = linkRepository
                .findByStudent_IdAndGuardian_Id(student.getId(), guardian.getId()).orElseThrow();

        assertThat(updated.getRelationship()).isEqualTo(GuardianRelationship.LEGAL_GUARDIAN);
        assertThat(updated.isPrimaryContact()).isFalse();
    }

    @Test
    void administratorShouldDeleteLink() throws Exception {
        Student student = persistStudent();
        Guardian guardian = persistGuardian("Maria da Silva", "52998224725");

        persistLink(student, guardian, true);

        mockMvc.perform(
                        delete("/api/students/{studentId}/guardians/{guardianId}",
                                student.getId(),
                                guardian.getId()
                        ).with(withRole("ADMINISTRATOR"))).andExpect(status().isNoContent());
        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void duplicateLinkShouldBeRejected() throws Exception {
        Student student = persistStudent();
        Guardian guardian = persistGuardian("Maria da Silva", "52998224725");

        persistLink(student, guardian, true);

        mockMvc.perform(
                        post("/api/students/{studentId}/guardians", student.getId())
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createLinkRequest(
                                        guardian.getId(),
                                        "MOTHER",
                                        true,
                                        true
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Student guardian link conflict"));

        assertThat(linkRepository.count()).isEqualTo(1);
    }

    @Test
    void secondPrimaryContactShouldBeRejected() throws Exception {
        Student student = persistStudent();
        Guardian firstGuardian = persistGuardian("Maria da Silva", "52998224725");
        Guardian secondGuardian = persistGuardian("Carlos da Silva", "11144477735");

        persistLink(student, firstGuardian, true);

        mockMvc.perform(
                        post("/api/students/{studentId}/guardians", student.getId())
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createLinkRequest(
                                        secondGuardian.getId(),
                                        "FATHER",
                                        true,
                                        true
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Student already has a primary contact"));

        assertThat(linkRepository.count()).isEqualTo(1);
    }

    @Test
    void inactiveGuardianShouldBeRejected() throws Exception {
        Student student = persistStudent();

        Guardian guardian = persistGuardian(
                "Maria da Silva",
                "52998224725"
        );

        guardian.deactivate();
        guardianRepository.saveAndFlush(guardian);

        mockMvc.perform(
                        post("/api/students/{studentId}/guardians", student.getId())
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createLinkRequest(
                                        guardian.getId(),
                                        "MOTHER",
                                        true,
                                        true
                                ))
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Inactive student or guardian"));

        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void missingLinkShouldReturnNotFound() throws Exception {
        mockMvc.perform(
                        delete("/api/students/{studentId}/guardians/{guardianId}",
                                UUID.randomUUID(),
                                UUID.randomUUID())
                                .with(withRole("ADMINISTRATOR"))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title")
                        .value("Student guardian link not found"));
    }

    @Test
    void incompleteRequestShouldBeRejected() throws Exception {
        Student student = persistStudent();
        mockMvc.perform(
                        post("/api/students/{studentId}/guardians", student.getId())
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());

        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(
                        get("/api/students/{studentId}/guardians", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private Student persistStudent() {
        Student student = new Student(
                "João da Silva",
                "João",
                LocalDate.of(2015, 3, 12),
                "MAT-2026-001",
                2026,
                "5 ano",
                "Turma A"
        );
        return studentRepository.saveAndFlush(student);
    }

    private Guardian persistGuardian(String fullName, String cpf) {
        Guardian guardian = new Guardian(
                fullName,
                cpf,
                fullName.toLowerCase()
                        .replace(" ", ".") + "@example.com",
                "(47) 99999-9999",
                null
        );

        return guardianRepository.saveAndFlush(guardian);
    }

    private StudentGuardian persistLink(
            Student student,
            Guardian guardian,
            boolean primaryContact
    ) {
        StudentGuardian link = new StudentGuardian(
                student,
                guardian,
                GuardianRelationship.MOTHER,
                true,
                primaryContact
        );
        return linkRepository.saveAndFlush(link);
    }

    private static String createLinkRequest(
            UUID guardianId,
            String relationship,
            boolean legalGuardian,
            boolean primaryContact
    ) {
        return """
                {
                  "guardianId": "%s",
                  "relationship": "%s",
                  "legalGuardian": %s,
                  "primaryContact": %s
                }
                """.formatted(
                guardianId,
                relationship,
                legalGuardian,
                primaryContact
        );
    }

    private static RequestPostProcessor withRole(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}