package com.github.caetanoog18.conectatea.student;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
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

    @Test
    void administratorShouldCreateStudent() throws Exception {
        mockMvc.perform(
                        post("/api/students")
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(studentRequestBody(
                                        "mat-2026-001",
                                        "João da Silva"
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fullName")
                        .value("João da Silva"))
                .andExpect(jsonPath("$.enrollmentNumber")
                        .value("MAT-2026-001"))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(studentRepository.count()).isEqualTo(1);
    }

    @Test
    void coordinatorShouldCreateStudent() throws Exception {
        mockMvc.perform(
                        post("/api/students")
                                .with(withRole(
                                        "PEDAGOGICAL_COORDINATOR"
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(studentRequestBody(
                                        "MAT-2026-002",
                                        "Maria da Silva"
                                ))
                )
                .andExpect(status().isCreated());

        assertThat(studentRepository.count()).isEqualTo(1);
    }

    @Test
    void teacherShouldNotAccessStudentManagement() throws Exception {
        mockMvc.perform(
                        post("/api/students")
                                .with(withRole("TEACHER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(studentRequestBody(
                                        "MAT-2026-001",
                                        "João da Silva"
                                ))
                )
                .andExpect(status().isForbidden());

        assertThat(studentRepository.count()).isZero();
    }

    @Test
    void administratorShouldListStudents() throws Exception {
        persistStudent("MAT-2026-001", "João da Silva");

        mockMvc.perform(
                        get("/api/students")
                                .with(withRole("ADMINISTRATOR"))
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.content[0].fullName")
                        .value("João da Silva"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void administratorShouldFindStudentById() throws Exception {
        Student student = persistStudent(
                "MAT-2026-001",
                "João da Silva"
        );

        mockMvc.perform(
                        get("/api/students/{studentId}", student.getId())
                                .with(withRole("ADMINISTRATOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(student.getId().toString()))
                .andExpect(jsonPath("$.birthDate")
                        .value("2015-03-12"))
                .andExpect(jsonPath("$.gradeLevel")
                        .value("5º ano"));
    }

    @Test
    void administratorShouldUpdateStudent() throws Exception {
        Student student = persistStudent(
                "MAT-2026-001",
                "João da Silva"
        );

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

        mockMvc.perform(
                        put(
                                "/api/students/{studentId}",
                                student.getId()
                        )
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName")
                        .value("João Pedro da Silva"))
                .andExpect(jsonPath("$.className")
                        .value("Turma B"));

        Student updated = studentRepository
                .findById(student.getId())
                .orElseThrow();

        assertThat(updated.getFullName())
                .isEqualTo("João Pedro da Silva");
        assertThat(updated.getClassName()).isEqualTo("Turma B");
    }

    @Test
    void administratorShouldDeactivateStudent() throws Exception {
        Student student = persistStudent(
                "MAT-2026-001",
                "João da Silva"
        );

        mockMvc.perform(
                        patch(
                                "/api/students/{studentId}/status",
                                student.getId()
                        )
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "active": false
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        Student updated = studentRepository
                .findById(student.getId())
                .orElseThrow();

        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void duplicateEnrollmentNumberShouldBeRejected() throws Exception {
        persistStudent("MAT-2026-001", "João da Silva");

        mockMvc.perform(
                        post("/api/students")
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(studentRequestBody(
                                        "mat-2026-001",
                                        "Outro estudante"
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value(
                                "Enrollment number already in use"
                        ));

        assertThat(studentRepository.count()).isEqualTo(1);
    }

    @Test
    void missingStudentShouldReturnNotFound() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/students/{studentId}",
                                UUID.randomUUID()
                        )
                                .with(withRole("ADMINISTRATOR"))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title")
                        .value("Student not found"));
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void statusUpdateWithoutActiveShouldBeRejected() throws Exception {
        mockMvc.perform(
                        patch(
                                "/api/students/{studentId}/status",
                                UUID.randomUUID()
                        )
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());
    }

    private Student persistStudent(
            String enrollmentNumber,
            String fullName
    ) {
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

    private static String studentRequestBody(
            String enrollmentNumber,
            String fullName
    ) {
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
        return jwt().authorities(
                new SimpleGrantedAuthority("ROLE_" + role)
        );
    }
}