package com.github.caetanoog18.conectatea.audit;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.audit.application.AuditedOperation;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditEvent;
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
import com.github.caetanoog18.conectatea.observation.api.dto.CreateObservationRequest;
import com.github.caetanoog18.conectatea.observation.application.ObservationService;
import com.github.caetanoog18.conectatea.observation.domain.Observation;
import com.github.caetanoog18.conectatea.observation.infrastructure.ObservationRepository;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import com.github.caetanoog18.conectatea.timeline.application.TimelineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@Transactional
class AuditIntegrationTest {
    @Autowired private ObservationService observationService;
    @Autowired private TimelineService timelineService;
    @Autowired private AuditedOperation auditedOperation;
    @Autowired private ObservationRepository observationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private GuardianRepository guardianRepository;
    @Autowired private StudentGuardianRepository guardianLinkRepository;
    @Autowired private StudentProfessionalLinkRepository linkRepository;
    @Autowired private ConsentTermRepository consentRepository;

    @MockitoSpyBean
    private AuditEventRepository auditRepository;

    @Test
    void shouldCommitAuditForAllFourOperations() {
        Fixture fixture = fixture(true);
        UUID studentId = fixture.student().getId();
        String email = fixture.teacher().getEmail();

        var observation = observationService.create(studentId, request(), email);

        observationService.findAll(studentId, 0, 20, email);
        observationService.findById(studentId, observation.id(), email);

        timelineService.findByStudent(studentId, null, null, 0, 20, email);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        var events = events(studentId);

        assertThat(events).hasSize(4);

        assertThat(events)
                .extracting(AuditEvent::getAction)
                .containsExactlyInAnyOrder(
                        AuditAction.OBSERVATION_CREATE,
                        AuditAction.OBSERVATION_LIST,
                        AuditAction.OBSERVATION_READ,
                        AuditAction.TIMELINE_READ
                );

        assertThat(events)
                .extracting(AuditEvent::getOutcome)
                .containsOnly(AuditOutcome.SUCCESS);

        assertThat(events)
                .extracting(AuditEvent::getActorUserId)
                .containsOnly(fixture.teacher().getId());

        assertThat(events)
                .filteredOn(event ->
                        event.getAction() == AuditAction.OBSERVATION_CREATE)
                .extracting(AuditEvent::getResourceId)
                .containsExactly(observation.id());

        assertThat(observationRepository.findById(observation.id()))
                .isPresent();
    }

    @Test
    void deniedEventShouldSurviveOuterRollback() {
        Fixture fixture = fixture(false);
        UUID studentId = fixture.student().getId();
        UUID actorId = fixture.teacher().getId();

        assertThatThrownBy(() ->
                timelineService.findByStudent(
                        studentId,
                        null,
                        null,
                        0,
                        20,
                        fixture.teacher().getEmail()
                )
        ).isInstanceOf(AccessDeniedException.class);

        rollbackTestTransaction();

        assertThat(studentRepository.findById(studentId)).isEmpty();

        var events = events(studentId);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getOutcome()).isEqualTo(AuditOutcome.DENIED);
        assertThat(events.getFirst().getActorUserId()).isEqualTo(actorId);
    }

    @Test
    void invalidPaginationShouldProduceFailureEvent() {
        Fixture fixture = fixture(true);
        UUID studentId = fixture.student().getId();

        assertThatThrownBy(() ->
                observationService.findAll(studentId, -1, 20, fixture.teacher().getEmail()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception)
                                .getStatusCode().value())
                        .isEqualTo(400));

        rollbackTestTransaction();

        var events = events(studentId);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(events.getFirst().getAction()).isEqualTo(AuditAction.OBSERVATION_LIST);
    }

    @Test
    void businessChangeAndSuccessEventShouldRollbackTogether() {
        Fixture fixture = fixture(true);
        UUID studentId = fixture.student().getId();

        var observation = observationService.create(studentId, request(), fixture.teacher().getEmail());

        assertThat(events(studentId)).hasSize(1);

        rollbackTestTransaction();

        assertThat(observationRepository.findById(observation.id())).isEmpty();
        assertThat(events(studentId)).isEmpty();
    }

    @Test
    void failureAfterInsertShouldRollbackDataButKeepFailureEvent() {
        Fixture fixture = fixture(false);
        UUID studentId = fixture.student().getId();

        AtomicReference<UUID> insertedId = new AtomicReference<>();

        assertThatThrownBy(() ->
                auditedOperation.<Observation>execute(
                        AuditAction.OBSERVATION_CREATE,
                        studentId,
                        null,
                        fixture.teacher().getEmail(),
                        () -> {
                            Observation saved = observationRepository
                                    .saveAndFlush(
                                            new Observation(
                                                    fixture.student(),
                                                    fixture.teacher(),
                                                    ConsentPurpose.EDUCATIONAL_SUPPORT,
                                                    "Teste de rollback",
                                                    "Conteudo ficticio.",
                                                    Instant.parse("2026-01-10T14:00:00Z")
                                            )
                                    );

                            insertedId.set(saved.getId());

                            throw new IllegalStateException("Simulated business failure");
                        },
                        Observation::getId
                )
        ).isInstanceOf(IllegalStateException.class);

        rollbackTestTransaction();

        assertThat(insertedId.get()).isNotNull();
        assertThat(observationRepository.findById(insertedId.get())).isEmpty();

        var events = events(studentId);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getOutcome()).isEqualTo(AuditOutcome.FAILURE);
    }

    @Test
    void unavailableAuditShouldPreventSuccessfulOperation() {
        Fixture fixture = fixture(true);
        UUID studentId = fixture.student().getId();

        doThrow(new DataIntegrityViolationException("Simulated audit storage failure"))
                .when(auditRepository)
                .save(any(AuditEvent.class));

        assertThatThrownBy(() -> observationService.create(
                studentId,
                request(),
                fixture.teacher().getEmail()
                )
        )
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(
                        ((ResponseStatusException) exception).getStatusCode().value()).isEqualTo(503));

        rollbackTestTransaction();

        var observations = observationRepository
                .findAllByStudent_IdAndPurposeOrderByOccurredAtDescIdDesc(
                        studentId,
                        ConsentPurpose.EDUCATIONAL_SUPPORT,
                        PageRequest.of(0, 20)
                );

        assertThat(observations.getTotalElements()).isZero();
        assertThat(events(studentId)).isEmpty();
    }

    @Test
    void requestIdShouldBeGeneratedByServerAndReusedWithinRequest() {
        Fixture fixture = fixture(true);

        String suppliedId = UUID.randomUUID().toString();

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        httpRequest.addHeader("X-Request-ID", suppliedId);

        var previousAttributes = RequestContextHolder.getRequestAttributes();
        var attributes = new ServletRequestAttributes(httpRequest, httpResponse);

        try {
            RequestContextHolder.setRequestAttributes(attributes);

            timelineService.findByStudent(
                    fixture.student().getId(),
                    null, null, 0, 20,
                    fixture.teacher().getEmail()
            );

            observationService.findAll(
                    fixture.student().getId(),
                    0, 20,
                    fixture.teacher().getEmail()
            );

            String generatedId = httpResponse.getHeader("X-Request-ID");

            assertThat(generatedId).isNotNull().isNotEqualTo(suppliedId);

            assertThat(events(fixture.student().getId()))
                    .extracting(AuditEvent::getRequestId)
                    .containsOnly(UUID.fromString(generatedId));
        } finally {
            attributes.requestCompleted();

            if (previousAttributes == null) {
                RequestContextHolder.resetRequestAttributes();
            } else {
                RequestContextHolder.setRequestAttributes(previousAttributes);
            }
        }
    }

    private List<AuditEvent> events(UUID studentId) {
        return auditRepository
                .findAllByStudentIdOrderByOccurredAtDescIdDesc(studentId, PageRequest.of(0, 100))
                .getContent();
    }

    private static void rollbackTestTransaction() {
        TestTransaction.flagForRollback();
        TestTransaction.end();
    }

    private static CreateObservationRequest request() {
        return new CreateObservationRequest(
                "Observacao de teste",
                "Conteudo ficticio que nao vai ser copiado para a auditoria.",
                Instant.parse("2026-01-10T14:00:00Z")
        );
    }

    private Fixture fixture(boolean withConsent) {
        String tag = UUID.randomUUID().toString();

        User administrator = userRepository.saveAndFlush(
                new User(
                        "Administrador Teste",
                        "admin-" + tag + "@example.com",
                        "temporary-password-hash",
                        UserRole.ADMINISTRATOR
                )
        );

        User teacher = userRepository.saveAndFlush(
                new User(
                        "Professor Teste",
                        "teacher-" + tag + "@example.com",
                        "temporary-password-hash",
                        UserRole.TEACHER
                )
        );

        Student student = studentRepository.saveAndFlush(
                new Student(
                        "Estudante Teste",
                        null,
                        LocalDate.of(2015, 5, 10),
                        "AUD-" + tag,
                        2026,
                        "5 ano",
                        "Turma Teste"
                )
        );

        Guardian guardian = guardianRepository.saveAndFlush(
                new Guardian(
                        "Responsavel Teste",
                        null,
                        "guardian-" + tag + "@example.com",
                        "00000000000",
                        null
                )
        );

        StudentGuardian guardianLink = guardianLinkRepository.saveAndFlush(
                new StudentGuardian(
                        student,
                        guardian,
                        GuardianRelationship.LEGAL_GUARDIAN,
                        true,
                        true
                )
        );

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        linkRepository.saveAndFlush(
                new StudentProfessionalLink(
                        student,
                        teacher,
                        today.minusDays(1),
                        administrator.getId()
                )
        );

        if (withConsent) {
            consentRepository.saveAndFlush(
                    new ConsentTerm(
                            guardianLink,
                            Set.of(
                                    ConsentPurpose.EDUCATIONAL_SUPPORT,
                                    ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
                            ),
                            "test-1.0",
                            Instant.now().minusSeconds(60),
                            today.plusDays(30),
                            administrator.getId()
                    )
            );
        }

        return new Fixture(student, teacher);
    }

    private record Fixture(Student student, User teacher) {
    }
}