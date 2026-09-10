package com.github.caetanoog18.conectatea.report;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditOutcome;
import com.github.caetanoog18.conectatea.audit.infrastructure.AuditEventRepository;
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
import com.github.caetanoog18.conectatea.report.api.dto.StudentReportResponse;
import com.github.caetanoog18.conectatea.report.infrastructure.StudentReportPdfRenderer;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;


@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@AutoConfigureMockMvc
@Transactional
class StudentReportIntegrationTest {
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-02-01T00:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private GuardianRepository guardianRepository;
    @Autowired private StudentGuardianRepository guardianLinkRepository;
    @Autowired private StudentProfessionalLinkRepository professionalLinkRepository;
    @Autowired private ConsentTermRepository consentRepository;
    @Autowired private ObservationRepository observationRepository;
    @Autowired private AuditEventRepository auditRepository;

    private User administrator;
    private User teacher;
    private Student student;
    private ConsentTerm consent;
    @MockitoSpyBean
    private StudentReportPdfRenderer pdfRenderer;

    @BeforeEach
    void setUp() {
        administrator = createUser(UserRole.ADMINISTRATOR);
        teacher = createUser(UserRole.TEACHER);
        student = createStudent();

        var guardian = guardianRepository.saveAndFlush(
                new Guardian(
                        "Responsavel Teste",
                        null,
                        "guardian-" + UUID.randomUUID() + "@example.com",
                        "00000000000",
                        null
                )
        );

        var guardianLink = guardianLinkRepository.saveAndFlush(
                new StudentGuardian(
                        student,
                        guardian,
                        GuardianRelationship.LEGAL_GUARDIAN,
                        true,
                        false
                )
        );

        professionalLinkRepository.saveAndFlush(
                new StudentProfessionalLink(
                        student,
                        teacher,
                        LocalDate.now(ZoneOffset.UTC).minusDays(1),
                        administrator.getId()
                )
        );

        consent = consentRepository.saveAndFlush(
                new ConsentTerm(
                        guardianLink,
                        Set.of(
                                ConsentPurpose.EDUCATIONAL_SUPPORT,
                                ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM,
                                ConsentPurpose.REPORT_GENERATION
                        ),
                        "test-1.0",
                        Instant.now().minusSeconds(60),
                        LocalDate.now(ZoneOffset.UTC).plusDays(30),
                        administrator.getId()
                )
        );
    }

    @Test
    void shouldFilterOrderAndAuditReport() throws Exception {
        saveObservation(
                student, teacher,
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                "Segundo registro", FROM.plusSeconds(3600)
        );

        saveObservation(
                student, teacher,
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                "Primeiro registro", FROM
        );

        saveObservation(
                student, teacher,
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                "Antes do periodo", FROM.minusSeconds(1)
        );

        saveObservation(
                student, teacher,
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                "No limite final", TO
        );

        var physician = createUser(UserRole.PHYSICIAN);

        saveObservation(
                student, physician,
                ConsentPurpose.MULTIPROFESSIONAL_MONITORING,
                "Outra finalidade", FROM.plusSeconds(1800)
        );

        saveObservation(createStudent(), teacher, ConsentPurpose.EDUCATIONAL_SUPPORT, "Outro estudante", FROM);

        var result = mockMvc.perform(
                        reportRequest(FROM, TO).with(jwt().jwt(token -> token.subject(teacher.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.id").value(student.getId().toString()))
                .andExpect(jsonPath("$.generatedBy.id").value(teacher.getId().toString()))
                .andExpect(jsonPath("$.purpose").value("EDUCATIONAL_SUPPORT"))
                .andExpect(jsonPath("$.totalObservations").value(2))
                .andExpect(jsonPath("$.observations.length()").value(2))
                .andExpect(jsonPath("$.observations[0].title").value("Primeiro registro"))
                .andExpect(jsonPath("$.observations[1].title").value("Segundo registro"))
                .andExpect(jsonPath("$.generatedBy.passwordHash").doesNotExist())
                .andReturn();

        assertAudit(result, AuditOutcome.SUCCESS);

        UUID requestId = UUID.fromString(result.getResponse().getHeader("X-Request-ID"));

        var event = auditRepository.findAllByRequestIdOrderByOccurredAtAscIdAsc(requestId).getFirst();
        assertThat(event.getResourceId()).isNotNull();

        jsonPath("$.reportId").value(event.getResourceId().toString()).match(result);
    }

    @Test
    void emptyPeriodShouldProduceEmptyReport() throws Exception {
        mockMvc.perform(reportRequest(FROM, TO).with(jwt().jwt(token -> token.subject(teacher.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalObservations").value(0))
                .andExpect(jsonPath("$.observations").isEmpty());
    }

    @Test
    void revokedConsentShouldBeDeniedAndAudited() throws Exception {
        consent.revoke(Instant.now(), administrator.getId(), "Permission withdrawn for test");
        consentRepository.saveAndFlush(consent);

        var result = mockMvc.perform(
                        reportRequest(FROM, TO).with(jwt().jwt(token -> token.subject(teacher.getEmail()))))
                .andExpect(status().isForbidden())
                .andReturn();

        assertAudit(result, AuditOutcome.DENIED);
    }

    @Test
    void administratorShouldNotBypassAuthorization() throws Exception {
        mockMvc.perform(
                reportRequest(FROM, TO)
                        .with(jwt().jwt(token -> token.subject(administrator.getEmail()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(reportRequest(FROM, TO)).andExpect(status().isUnauthorized());
    }

    @Test
    void invertedPeriodShouldBeRejectedAndAudited() throws Exception {
        var result = mockMvc.perform(reportRequest(TO, FROM)
                        .with(jwt().jwt(token -> token.subject(teacher.getEmail()))))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertAudit(result, AuditOutcome.FAILURE);
    }

    @Test
    void periodLongerThan366DaysShouldBeRejected() throws Exception {
        mockMvc.perform(reportRequest(FROM, FROM.plusSeconds(367L * 86400))
                        .with(jwt().jwt(token -> token.subject(teacher.getEmail()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedReportShouldFailInsteadOfTruncating() throws Exception {
        for (int i = 0; i < 501; i++) {
            observationRepository.save(
                    new Observation(
                            student,
                            teacher,
                            ConsentPurpose.EDUCATIONAL_SUPPORT,
                            "Registro " + i,
                            "Conteudo ficticio para teste",
                            FROM.plusSeconds(i)
                    )
            );
        }

        observationRepository.flush();

        var result = mockMvc.perform(
                reportRequest(FROM, TO).with(jwt().jwt(token -> token.subject(teacher.getEmail()))))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.observations").doesNotExist())
                .andReturn();

        assertAudit(result, AuditOutcome.FAILURE);
    }

    @Test
    void shouldExportPdfAndAuditOnlyPdfAction() throws Exception {
        saveObservation(
                student,
                teacher,
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                "Participação na educação",
                FROM
        );

        var physician = createUser(UserRole.PHYSICIAN);

        saveObservation(
                student,
                physician,
                ConsentPurpose.MULTIPROFESSIONAL_MONITORING,
                "CONTEUDO_NAO_AUTORIZADO",
                FROM
        );

        var result = mockMvc.perform(
                        pdfRequest(FROM, TO).with(jwt().jwt(token -> token.subject(teacher.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .startsWith("attachment;")
                .contains(".pdf");

        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");

        String reportId = result.getResponse().getHeader("X-Report-ID");
        assertThat(reportId).isNotBlank();

        try (PdfReader reader = new PdfReader(
                result.getResponse().getContentAsByteArray()
        )) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();

            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page));
            }

            assertThat(text.toString())
                    .contains("Participação na educação")
                    .contains(reportId)
                    .doesNotContain("CONTEUDO_NAO_AUTORIZADO");
        }

        assertPdfAudit(result, AuditOutcome.SUCCESS);

        UUID requestId = UUID.fromString(result.getResponse().getHeader("X-Request-ID"));

        var event = auditRepository.findAllByRequestIdOrderByOccurredAtAscIdAsc(requestId).getFirst();

        assertThat(event.getResourceId()).isEqualTo(UUID.fromString(reportId));
    }

    @Test
    void pdfRenderingFailureShouldNotCreateSuccessAudit() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF generation failed"))
                .when(pdfRenderer)
                .render(any(StudentReportResponse.class));

        var result = mockMvc.perform(
                        pdfRequest(FROM, TO).with(jwt().jwt(token -> token.subject(teacher.getEmail()))))
                .andExpect(status().isInternalServerError())
                .andReturn();

        assertThat(result.getResponse().getHeader("Content-Disposition")).isNull();
        assertThat(result.getResponse().getHeader("X-Report-ID")).isNull();

        assertPdfAudit(result, AuditOutcome.FAILURE);
    }

    @Test
    void revokedConsentShouldBlockPdfExport() throws Exception {
        consent.revoke(Instant.now(), administrator.getId(), "PDF permission withdrawn");
        consentRepository.saveAndFlush(consent);

        var result = mockMvc.perform(
                        pdfRequest(FROM, TO).with(jwt().jwt(token -> token.subject(teacher.getEmail()))))
                .andExpect(status().isForbidden())
                .andReturn();

        assertPdfAudit(result, AuditOutcome.DENIED);
    }

    @Test
    void administratorShouldNotBypassPdfAuthorization() throws Exception {
        mockMvc.perform(
                        pdfRequest(FROM, TO).with(jwt().jwt(token -> token.subject(administrator.getEmail()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousPdfRequestShouldBeRejected() throws Exception {
        mockMvc.perform(pdfRequest(FROM, TO)).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPdfPeriodShouldBeRejected() throws Exception {
        var result = mockMvc.perform(
                pdfRequest(TO, FROM).with(jwt().jwt(token -> token.subject(teacher.getEmail()))))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertPdfAudit(result, AuditOutcome.FAILURE);
    }

    private MockHttpServletRequestBuilder pdfRequest(Instant from, Instant to) {
        return post("/api/me/students/{studentId}/reports/pdf", student.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "from": "%s",
                      "to": "%s"
                    }
                    """.formatted(from, to));
    }

    private void assertPdfAudit(MvcResult result, AuditOutcome expectedOutcome) {
        String header = result.getResponse().getHeader("X-Request-ID");
        assertThat(header).isNotBlank();

        var events = auditRepository.findAllByRequestIdOrderByOccurredAtAscIdAsc(UUID.fromString(header));
        assertThat(events).hasSize(1);
        var event = events.getFirst();

        assertThat(event.getAction()).isEqualTo(AuditAction.REPORT_PDF_EXPORT);
        assertThat(event.getOutcome()).isEqualTo(expectedOutcome);
        assertThat(event.getStudentId()).isEqualTo(student.getId());
        assertThat(event.getActorUserId()).isEqualTo(teacher.getId());
    }

    private MockHttpServletRequestBuilder reportRequest(Instant from, Instant to) {
        return post("/api/me/students/{studentId}/reports", student.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "from": "%s",
                          "to": "%s"
                        }
                        """.formatted(from, to));
    }

    private void assertAudit(MvcResult result, AuditOutcome expectedOutcome) {
        String header = result.getResponse().getHeader("X-Request-ID");
        assertThat(header).isNotBlank();

        var events = auditRepository.findAllByRequestIdOrderByOccurredAtAscIdAsc(UUID.fromString(header));

        assertThat(events).hasSize(1);

        var event = events.getFirst();

        assertThat(event.getAction()).isEqualTo(AuditAction.REPORT_GENERATE);
        assertThat(event.getOutcome()).isEqualTo(expectedOutcome);
        assertThat(event.getStudentId()).isEqualTo(student.getId());
        assertThat(event.getActorUserId()).isEqualTo(teacher.getId());
    }

    private User createUser(UserRole role) {
        return userRepository.saveAndFlush(
                new User(
                        "Usuario Teste",
                        "report-" + UUID.randomUUID() + "@example.com",
                        "temporary-password-hash",
                        role
                )
        );
    }

    private Student createStudent() {
        return studentRepository.saveAndFlush(
                new Student(
                        "Estudante Teste",
                        null,
                        LocalDate.of(2015, 5, 10),
                        "REPORT-" + UUID.randomUUID(),
                        2026,
                        "5 ano",
                        "Turma Teste"
                )
        );
    }

    private void saveObservation(
            Student target,
            User author,
            ConsentPurpose purpose,
            String title,
            Instant occurredAt
    ) {
        observationRepository.saveAndFlush(
                new Observation(target, author, purpose, title, "Conteudo ficticio para teste", occurredAt));
    }
}