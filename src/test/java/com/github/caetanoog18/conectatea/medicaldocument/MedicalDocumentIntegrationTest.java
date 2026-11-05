package com.github.caetanoog18.conectatea.medicaldocument;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditOutcome;
import com.github.caetanoog18.conectatea.audit.infrastructure.AuditEventRepository;
import com.github.caetanoog18.conectatea.careteam.domain.StudentProfessionalLink;
import com.github.caetanoog18.conectatea.careteam.infrastructure.StudentProfessionalLinkRepository;
import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.consent.domain.ConsentTerm;
import com.github.caetanoog18.conectatea.consent.infrastructure.ConsentTermRepository;
import com.github.caetanoog18.conectatea.guardian.domain.*;
import com.github.caetanoog18.conectatea.guardian.infrastructure.GuardianRepository;
import com.github.caetanoog18.conectatea.guardian.infrastructure.StudentGuardianRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.medicaldocument.infrastructure.MedicalDocumentRepository;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import com.github.caetanoog18.conectatea.support.TestCpf;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.Document;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password=",
        "app.bootstrap.admin.cpf="
})
@AutoConfigureMockMvc
@Transactional
class MedicalDocumentIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private StudentRepository students;

    @Autowired
    private GuardianRepository guardians;

    @Autowired
    private StudentGuardianRepository guardianLinks;

    @Autowired
    private StudentProfessionalLinkRepository links;

    @Autowired
    private ConsentTermRepository consents;

    @Autowired
    private MedicalDocumentRepository documents;

    @Autowired
    private AuditEventRepository audit;

    @Autowired
    private EntityManager entityManager;

    private User physician;
    private Student student;
    private StudentProfessionalLink professionalLink;
    private ConsentTerm consent;

    @BeforeEach
    void setUp() {
        physician = user("Médica de teste", UserRole.PHYSICIAN);
        student = newStudent("MED-1");

        Guardian guardian = guardians.saveAndFlush(new Guardian(
                "Responsável de teste",
                TestCpf.next(),
                null,
                "47999999999",
                null
        ));

        var guardianLink = guardianLinks.saveAndFlush(new StudentGuardian(
                student,
                guardian,
                GuardianRelationship.MOTHER,
                true,
                true
        ));

        professionalLink = links.saveAndFlush(new StudentProfessionalLink(
                student,
                physician,
                today().minusDays(1),
                physician.getId()
        ));

        consent = consents.saveAndFlush(new ConsentTerm(
                guardianLink,
                Set.of(
                        ConsentPurpose.EDUCATIONAL_SUPPORT,
                        ConsentPurpose.MULTIPROFESSIONAL_MONITORING,
                        ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
                ),
                "1",
                Instant.now().minusSeconds(60),
                today().plusYears(1),
                physician.getId()
        ));
    }

    @Test
    void physicianShouldUploadListAndDownloadPdf() throws Exception {
        byte[] pdf = validPdf();

        upload(
                new MockMultipartFile(
                        "file",
                        "laudo.pdf",
                        "application/pdf",
                        pdf
                ),
                physician
        )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Cache-Control",
                        containsString("no-store")
                ))
                .andExpect(jsonPath("$.title").value("Laudo médico"))
                .andExpect(jsonPath("$.issuedOn").value("2020-01-02"))
                .andExpect(jsonPath("$.uploadedByUserId").value(
                        physician.getId().toString()
                ))
                .andExpect(jsonPath("$.content").doesNotExist());

        UUID id = documents.findAll().getFirst().getId();

        list(physician)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].content").doesNotExist());

        download(student.getId(), id, physician)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("attachment")
                ))
                .andExpect(header().string(
                        "Cache-Control",
                        containsString("no-store")
                ))
                .andExpect(content().bytes(pdf));

        var events = audit.findAllByStudentIdOrderByOccurredAtDescIdDesc(
                student.getId(),
                Pageable.unpaged()
        ).getContent();

        assertThat(events).anySatisfy(event -> {
            assertThat(event.getAction())
                    .isEqualTo(AuditAction.MEDICAL_DOCUMENT_UPLOAD);
            assertThat(event.getOutcome())
                    .isEqualTo(AuditOutcome.SUCCESS);
            assertThat(event.getResourceId())
                    .isEqualTo(id);
            assertThat(event.getActorUserId())
                    .isEqualTo(physician.getId());
        });
    }

    @Test
    void teacherShouldReadButNotUpload() throws Exception {
        UUID id = uploadValid();
        User teacher = user("Professora de teste", UserRole.TEACHER);

        links.saveAndFlush(new StudentProfessionalLink(
                student,
                teacher,
                today().minusDays(1),
                physician.getId()
        ));

        list(teacher).andExpect(status().isOk());

        download(student.getId(), id, teacher)
                .andExpect(status().isOk());

        upload(new MockMultipartFile("file", validPdf()), teacher)
                .andExpect(status().isForbidden());
    }

    @Test
    void forgedPhysicianAuthorityShouldNotAllowTeacherUpload() throws Exception {
        User teacher = user("Outra professora", UserRole.TEACHER);

        mvc.perform(multipart(base())
                        .file(new MockMultipartFile("file", validPdf()))
                        .param("title", "Laudo")
                        .param("issuedOn", "2020-01-02")
                        .with(jwt()
                                .jwt(token -> token.subject(teacher.getEmail()))
                                .authorities(new SimpleGrantedAuthority(
                                        "ROLE_PHYSICIAN"
                                ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorShouldNotHaveAutomaticClinicalAccess() throws Exception {
        User admin = user("Administrador", UserRole.ADMINISTRATOR);

        list(admin).andExpect(status().isForbidden());
    }

    @Test
    void legalGuardianShouldNotUseProfessionalEndpoints() throws Exception {
        User guardian = user("Responsável", UserRole.LEGAL_GUARDIAN);

        list(guardian).andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestShouldBeRejected() throws Exception {
        mvc.perform(get(base()))
                .andExpect(status().isUnauthorized());

        mvc.perform(multipart(base())
                        .file(new MockMultipartFile("file", validPdf()))
                        .param("title", "Laudo")
                        .param("issuedOn", "2020-01-02"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokedConsentShouldBlockStoredPdf() throws Exception {
        UUID id = uploadValid();

        consent.revoke(
                Instant.now(),
                physician.getId(),
                "Retirada de consentimento"
        );
        consents.saveAndFlush(consent);

        list(physician).andExpect(status().isForbidden());

        download(student.getId(), id, physician)
                .andExpect(status().isForbidden());

        assertThat(documents.count()).isEqualTo(1);
    }

    @Test
    void endedLinkShouldBlockDownload() throws Exception {
        UUID id = uploadValid();

        professionalLink.end(
                today(),
                physician.getId(),
                "Fim do acompanhamento"
        );
        links.saveAndFlush(professionalLink);

        download(student.getId(), id, physician)
                .andExpect(status().isForbidden());
    }

    @Test
    void missingConsentShouldBlockListAndUpload() throws Exception {
        consents.delete(consent);
        consents.flush();

        list(physician).andExpect(status().isForbidden());

        upload(new MockMultipartFile("file", validPdf()), physician)
                .andExpect(status().isForbidden());
    }

    @Test
    void inactivePhysicianShouldBeDenied() throws Exception {
        physician.deactivate();
        users.saveAndFlush(physician);

        list(physician).andExpect(status().isForbidden());
    }

    @Test
    void inactiveStudentShouldBeDenied() throws Exception {
        student.deactivate();
        students.saveAndFlush(student);

        list(physician).andExpect(status().isForbidden());
    }

    @Test
    void documentMustBelongToRequestedStudent() throws Exception {
        UUID id = uploadValid();
        var other = newStudent("MED-2");

        download(other.getId(), id, physician)
                .andExpect(status().isForbidden());

        download(student.getId(), UUID.randomUUID(), physician)
                .andExpect(status().isNotFound());
    }

    @Test
    void documentFromAnotherAuthorizedStudentShouldStillBeNotFound() throws Exception {
        UUID id = uploadValid();
        var other = newStudent("MED-OTHER-AUTHORIZED");

        var guardian = guardians.saveAndFlush(new Guardian(
                "Outra responsável",
                TestCpf.next(),
                null,
                "47999999999",
                null
        ));

        var guardianLink = guardianLinks.saveAndFlush(new StudentGuardian(
                other,
                guardian,
                GuardianRelationship.MOTHER,
                true,
                true
        ));

        links.saveAndFlush(new StudentProfessionalLink(
                other,
                physician,
                today().minusDays(1),
                physician.getId()
        ));

        consents.saveAndFlush(new ConsentTerm(
                guardianLink,
                Set.of(
                        ConsentPurpose.MULTIPROFESSIONAL_MONITORING,
                        ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
                ),
                "1",
                Instant.now().minusSeconds(60),
                today().plusYears(1),
                physician.getId()
        ));

        download(other.getId(), id, physician)
                .andExpect(status().isNotFound());
    }

    @Test
    void renamedTextShouldNotBeAcceptedAsPdf() throws Exception {
        upload(
                new MockMultipartFile(
                        "file",
                        "fake.pdf",
                        "application/pdf",
                        "not a PDF".getBytes()
                ),
                physician
        ).andExpect(status().isBadRequest());

        assertThat(documents.count()).isZero();
    }

    @Test
    void brokenPdfHeaderShouldNotBeEnough() throws Exception {
        upload(
                new MockMultipartFile(
                        "file",
                        "broken.pdf",
                        "application/pdf",
                        "%PDF-1.4 invalid".getBytes()
                ),
                physician
        ).andExpect(status().isBadRequest());
    }

    @Test
    void emptyPdfShouldBeRejected() throws Exception {
        upload(new MockMultipartFile("file", new byte[0]), physician)
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedFileShouldBeRejected() throws Exception {
        upload(
                new MockMultipartFile(
                        "file",
                        new byte[10 * 1024 * 1024 + 1]
                ),
                physician
        ).andExpect(status().is(413));
    }

    @Test
    void futureIssueDateShouldBeRejected() throws Exception {
        mvc.perform(multipart(base())
                        .file(new MockMultipartFile("file", validPdf()))
                        .param("title", "Laudo")
                        .param("issuedOn", today().plusYears(1).toString())
                        .with(token(physician)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankTitleShouldBeRejected() throws Exception {
        mvc.perform(multipart(base())
                        .file(new MockMultipartFile("file", validPdf()))
                        .param("title", "   ")
                        .param("issuedOn", "2020-01-02")
                        .with(token(physician)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paginationShouldBeValidated() throws Exception {
        mvc.perform(get(base())
                        .param("page", "-1")
                        .with(token(physician)))
                .andExpect(status().isBadRequest());

        mvc.perform(get(base())
                        .param("size", "101")
                        .with(token(physician)))
                .andExpect(status().isBadRequest());
    }

    private UUID uploadValid() throws Exception {
        upload(
                new MockMultipartFile(
                        "file",
                        "laudo.pdf",
                        "application/pdf",
                        validPdf()
                ),
                physician
        ).andExpect(status().isCreated());

        return documents.findAll().getFirst().getId();
    }

    private ResultActions upload(
            MockMultipartFile file,
            User user
    ) throws Exception {
        return mvc.perform(multipart(base())
                .file(file)
                .param("title", " Laudo médico ")
                .param("issuedOn", "2020-01-02")
                .with(token(user)));
    }

    private ResultActions list(User user) throws Exception {
        entityManager.flush();

        return mvc.perform(get(base()).with(token(user)));
    }

    private ResultActions download(
            UUID studentId,
            UUID documentId,
            User user
    ) throws Exception {
        entityManager.flush();

        return mvc.perform(get(
                "/api/me/students/{studentId}/medical-documents/{documentId}/file",
                studentId,
                documentId
        ).with(token(user)));
    }

    private String base() {
        return "/api/me/students/" + student.getId() + "/medical-documents";
    }

    private static RequestPostProcessor token(User user) {
        return jwt()
                .jwt(token -> token.subject(user.getEmail()))
                .authorities(new SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name()
                ));
    }

    private User user(String name, UserRole role) {
        return users.saveAndFlush(new User(
                name,
                UUID.randomUUID() + "@example.com",
                "temporary-password-hash",
                role,
                TestCpf.next()
        ));
    }

    private Student newStudent(String enrollment) {
        return students.saveAndFlush(new Student(
                "Ana Souza",
                null,
                LocalDate.of(2015, 5, 10),
                enrollment,
                2026,
                "5º ano",
                "B",
                TestCpf.next()
        ));
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    static byte[] validPdf() throws Exception {
        var output = new ByteArrayOutputStream();
        var document = new Document();

        PdfWriter.getInstance(document, output);
        document.open();
        document.add(new Paragraph("Laudo de teste"));
        document.close();

        return output.toByteArray();
    }
}