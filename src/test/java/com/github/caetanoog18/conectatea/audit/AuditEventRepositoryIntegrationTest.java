package com.github.caetanoog18.conectatea.audit;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditEvent;
import com.github.caetanoog18.conectatea.audit.domain.AuditOutcome;
import com.github.caetanoog18.conectatea.audit.infrastructure.AuditEventRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@Transactional
class AuditEventRepositoryIntegrationTest {
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAuditMetadata() {
        UUID actorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID observationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        AuditEvent saved = auditEventRepository.save(
                new AuditEvent(
                        actorId,
                        AuditAction.OBSERVATION_READ,
                        AuditOutcome.SUCCESS,
                        studentId,
                        observationId,
                        requestId
                )
        );

        entityManager.flush();

        UUID eventId = saved.getId();

        entityManager.clear();

        AuditEvent loaded = auditEventRepository.findById(eventId).orElseThrow();

        assertThat(loaded.getActorUserId()).isEqualTo(actorId);
        assertThat(loaded.getAction()).isEqualTo(AuditAction.OBSERVATION_READ);
        assertThat(loaded.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(loaded.getStudentId()).isEqualTo(studentId);
        assertThat(loaded.getResourceId()).isEqualTo(observationId);
        assertThat(loaded.getRequestId()).isEqualTo(requestId);
        assertThat(loaded.getOccurredAt()).isNotNull();
    }

    @Test
    void shouldAllowDeniedAttemptWithoutKnownActor() {
        UUID attemptedStudentId = UUID.randomUUID();

        AuditEvent saved = auditEventRepository.save(
                new AuditEvent(
                        null,
                        AuditAction.TIMELINE_READ,
                        AuditOutcome.DENIED,
                        attemptedStudentId,
                        null,
                        UUID.randomUUID()
                )
        );

        entityManager.flush();

        UUID eventId = saved.getId();

        entityManager.clear();

        AuditEvent loaded = auditEventRepository.findById(eventId).orElseThrow();

        assertThat(loaded.getActorUserId()).isNull();
        assertThat(loaded.getStudentId()).isEqualTo(attemptedStudentId);
        assertThat(loaded.getResourceId()).isNull();
        assertThat(loaded.getOutcome()).isEqualTo(AuditOutcome.DENIED);
    }

    @Test
    void shouldFilterEventsByStudentAndRequest() {
        UUID actorId = UUID.randomUUID();
        UUID firstStudentId = UUID.randomUUID();
        UUID secondStudentId = UUID.randomUUID();
        UUID firstRequestId = UUID.randomUUID();
        UUID secondRequestId = UUID.randomUUID();

        auditEventRepository.save(
                new AuditEvent(
                        actorId,
                        AuditAction.OBSERVATION_LIST,
                        AuditOutcome.SUCCESS,
                        firstStudentId,
                        null,
                        firstRequestId
                )
        );

        auditEventRepository.save(
                new AuditEvent(
                        actorId,
                        AuditAction.TIMELINE_READ,
                        AuditOutcome.SUCCESS,
                        secondStudentId,
                        null,
                        secondRequestId
                )
        );

        entityManager.flush();
        entityManager.clear();

        var studentEvents = auditEventRepository
                .findAllByStudentIdOrderByOccurredAtDescIdDesc(firstStudentId, PageRequest.of(0, 20));

        assertThat(studentEvents.getTotalElements()).isEqualTo(1);

        assertThat(studentEvents.getContent())
                .extracting(AuditEvent::getStudentId)
                .containsExactly(firstStudentId);

        var requestEvents = auditEventRepository
                .findAllByRequestIdOrderByOccurredAtAscIdAsc(secondRequestId);

        assertThat(requestEvents).hasSize(1);
        assertThat(requestEvents.getFirst().getStudentId()).isEqualTo(secondStudentId);
        assertThat(requestEvents.getFirst().getAction()).isEqualTo(AuditAction.TIMELINE_READ);
    }

    @Test
    void shouldRequireRequestId() {
        assertThatThrownBy(() -> new AuditEvent(
                        UUID.randomUUID(),
                        AuditAction.OBSERVATION_CREATE,
                        AuditOutcome.SUCCESS,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Request ID is required");
    }
}