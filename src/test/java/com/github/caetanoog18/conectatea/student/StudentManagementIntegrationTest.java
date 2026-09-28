package com.github.caetanoog18.conectatea.student;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditEvent;
import com.github.caetanoog18.conectatea.audit.domain.AuditOutcome;
import com.github.caetanoog18.conectatea.audit.infrastructure.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class StudentManagementIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private AuditEventRepository auditEventRepository;


    @Test
    void administratorShouldCreateStudent() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/students")
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(studentRequestBody("mat-2026-001", "João da Silva")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fullName").value("João da Silva"))
                .andExpect(jsonPath("$.enrollmentNumber").value("MAT-2026-001"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        Student savedStudent = studentRepository.findByEnrollmentNumberIgnoreCase("MAT-2026-001").orElseThrow();

        AuditEvent event = auditEventFrom(result);

        assertThat(event.getAction()).isEqualTo(AuditAction.STUDENT_CREATE);
        assertThat(event.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.getStudentId()).isNull();
        assertThat(event.getResourceId()).isEqualTo(savedStudent.getId());

        assertThat(studentRepository.count()).isEqualTo(1);
    }

    @Test
    void coordinatorShouldCreateStudent() throws Exception {
        mockMvc.perform(
                        post("/api/students")
                                .with(withRole("PEDAGOGICAL_COORDINATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(studentRequestBody("MAT-2026-002", "Maria da Silva")))
                .andExpect(status().isCreated());

        assertThat(studentRepository.count()).isEqualTo(1);
    }

    @Test
    void teacherShouldNotAccessStudentManagement() throws Exception {
        mockMvc.perform(
                        post("/api/students")
                                .with(withRole("TEACHER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(studentRequestBody("MAT-2026-001", "João da Silva")))
                .andExpect(status().isForbidden());

        assertThat(studentRepository.count()).isZero();
    }

    @Test
    void administratorShouldListStudents() throws Exception {
        persistStudent("MAT-2026-001", "João da Silva");

        mockMvc.perform(get("/api/students")
                        .with(withRole("ADMINISTRATOR"))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("João da Silva"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void administratorShouldFindStudentById() throws Exception {
        Student student = persistStudent("MAT-2026-001", "João da Silva");

        mockMvc.perform(get("/api/students/{studentId}", student.getId()).with(withRole("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId().toString()))
                .andExpect(jsonPath("$.birthDate").value("2015-03-12"))
                .andExpect(jsonPath("$.gradeLevel").value("5º ano"));
    }

    @Test
    void administratorShouldUpdateStudent() throws Exception {
        Student student = persistStudent("MAT-2026-001", "João da Silva");

        String requestBody = """
                {
                  "fullName": "João Pedro da Silva",
                  "preferredName": "João",
                  "birthDate": "2015-03-12",
                  "enrollmentNumber": "mat-2026-001",
                  "schoolYear": 2026,
                  "gradeLevel": "5º ano",
                  "className": "Turma B"
                }
                """;

        MvcResult result = mockMvc.perform(
                        put("/api/students/{studentId}", student.getId())
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.fullName").value("João Pedro da Silva"))
                .andExpect(jsonPath("$.className").value("Turma B"))
                .andReturn();

        AuditEvent event = auditEventFrom(result);

        assertThat(event.getAction()).isEqualTo(AuditAction.STUDENT_UPDATE);
        assertThat(event.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.getStudentId()).isEqualTo(student.getId());
        assertThat(event.getResourceId()).isEqualTo(student.getId());

        Student updated = studentRepository.findById(student.getId()).orElseThrow();

        assertThat(updated.getFullName()).isEqualTo("João Pedro da Silva");
        assertThat(updated.getClassName()).isEqualTo("Turma B");
    }

    @Test
    void administratorShouldDeactivateStudent() throws Exception {
        Student student = persistStudent("MAT-2026-001", "João da Silva");

        MvcResult result = mockMvc.perform(
                        patch("/api/students/{studentId}/status", student.getId())
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "active": false
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.active").value(false))
                .andReturn();

        AuditEvent event = auditEventFrom(result);

        assertThat(event.getAction()).isEqualTo(AuditAction.STUDENT_STATUS_UPDATE);
        assertThat(event.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.getStudentId()).isEqualTo(student.getId());
        assertThat(event.getResourceId()).isEqualTo(student.getId());

        Student updated = studentRepository.findById(student.getId()).orElseThrow();

        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void duplicateEnrollmentNumberShouldBeRejected() throws Exception {
        persistStudent("MAT-2026-001", "João da Silva");

        mockMvc.perform(
                        post("/api/students")
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(studentRequestBody("mat-2026-001", "Outro estudante")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Enrollment number already in use"));

        assertThat(studentRepository.count()).isEqualTo(1);
    }

    @Test
    void missingStudentShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}", UUID.randomUUID()).with(withRole("ADMINISTRATOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Student not found"));
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
    }

    @Test
    void statusUpdateWithoutActiveShouldBeRejected() throws Exception {
        mockMvc.perform(
                        patch("/api/students/{studentId}/status", UUID.randomUUID())
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private Student persistStudent(String enrollmentNumber, String fullName) {
        Student student = new Student(
                fullName,
                "João",
                LocalDate.of(2015, 3, 12),
                enrollmentNumber,
                2026,
                "5º ano",
                "Turma A"
        );

        return studentRepository.saveAndFlush(student);
    }

    private static String studentRequestBody(String enrollmentNumber, String fullName) {
        return """
                {
                  "fullName": "%s",
                  "preferredName": "João",
                  "birthDate": "2015-03-12",
                  "enrollmentNumber": "%s",
                  "schoolYear": 2026,
                  "gradeLevel": "5º ano",
                  "className": "Turma A"
                }
                """.formatted(fullName, enrollmentNumber);
    }

    private static RequestPostProcessor withRole(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private AuditEvent auditEventFrom(MvcResult result) {
        String requestIdHeader = result.getResponse().getHeader("X-Request-ID");
        assertThat(requestIdHeader).isNotBlank();
        UUID requestId = UUID.fromString(requestIdHeader);
        var events = auditEventRepository.findAllByRequestIdOrderByOccurredAtAscIdAsc(requestId);
        assertThat(events).hasSize(1);
        return events.getFirst();
    }
}