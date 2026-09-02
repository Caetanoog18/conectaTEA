package com.github.caetanoog18.conectatea.audit;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditOutcome;
import com.github.caetanoog18.conectatea.audit.infrastructure.AuditEventRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
class AuditQueryIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditEventRepository auditRepository;

    private User administrator;
    private User teacher;
    private UUID studentId;
    private UUID requestId;

    @BeforeEach
    void setUp() {
        String tag = UUID.randomUUID().toString();

        administrator = userRepository.saveAndFlush(
                new User(
                        "Administrador Teste",
                        "audit-query-admin-" + tag + "@example.com",
                        "temporary-password-hash",
                        UserRole.ADMINISTRATOR
                )
        );

        teacher = userRepository.saveAndFlush(
                new User(
                        "Professor Teste",
                        "audit-query-teacher-" + tag + "@example.com",
                        "temporary-password-hash",
                        UserRole.TEACHER
                )
        );

        studentId = UUID.randomUUID();
        requestId = UUID.randomUUID();
    }

    @Test
    void administratorShouldReadMetadataAndGenerateAuditEvent() throws Exception {
        UUID eventId = seed(
                studentId,
                teacher.getId(),
                requestId,
                AuditAction.OBSERVATION_READ,
                AuditOutcome.SUCCESS,
                "2026-01-10T08:00:00Z"
        );

        var result = read("requestId", requestId.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(eventId.toString()))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].content").doesNotExist())
                .andExpect(header().exists("X-Request-ID"))
                .andReturn();

        UUID queryRequestId = UUID.fromString(
                result.getResponse().getHeader("X-Request-ID")
        );

        var events = auditRepository.findAllByRequestIdOrderByOccurredAtAscIdAsc(queryRequestId);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getAction()).isEqualTo(AuditAction.AUDIT_EVENTS_LIST);
        assertThat(events.getFirst().getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(events.getFirst().getActorUserId()).isEqualTo(administrator.getId());
    }

    @Test
    void shouldPaginateNewestFirst() throws Exception {
        UUID oldest = seed(
                studentId, teacher.getId(), requestId,
                AuditAction.OBSERVATION_READ, AuditOutcome.SUCCESS,
                "2026-01-10T08:00:00Z"
        );

        UUID middle = seed(
                studentId, teacher.getId(), requestId,
                AuditAction.OBSERVATION_READ, AuditOutcome.SUCCESS,
                "2026-01-10T12:00:00Z"
        );

        UUID newest = seed(
                studentId, teacher.getId(), requestId,
                AuditAction.OBSERVATION_READ, AuditOutcome.SUCCESS,
                "2026-01-10T18:00:00Z"
        );

        read("requestId", requestId.toString(), "page", "0", "size", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(newest.toString()))
                .andExpect(jsonPath("$.content[1].id").value(middle.toString()));

        read("requestId", requestId.toString(), "page", "1", "size", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(oldest.toString()));
    }

    @Test
    void shouldCombineFilters() throws Exception {
        UUID expected = seed(
                studentId, teacher.getId(), requestId,
                AuditAction.TIMELINE_READ, AuditOutcome.DENIED,
                "2026-01-10T12:00:00Z"
        );

        seed(
                UUID.randomUUID(), teacher.getId(), requestId,
                AuditAction.TIMELINE_READ, AuditOutcome.DENIED,
                "2026-01-10T12:00:00Z"
        );

        seed(
                studentId, administrator.getId(), requestId,
                AuditAction.TIMELINE_READ, AuditOutcome.DENIED,
                "2026-01-10T12:00:00Z"
        );

        seed(
                studentId, teacher.getId(), requestId,
                AuditAction.OBSERVATION_READ, AuditOutcome.DENIED,
                "2026-01-10T12:00:00Z"
        );

        seed(
                studentId, teacher.getId(), requestId,
                AuditAction.TIMELINE_READ, AuditOutcome.SUCCESS,
                "2026-01-10T12:00:00Z"
        );

        seed(
                studentId, teacher.getId(), UUID.randomUUID(),
                AuditAction.TIMELINE_READ, AuditOutcome.DENIED,
                "2026-01-10T12:00:00Z"
        );

        read(
                "studentId", studentId.toString(),
                "actorUserId", teacher.getId().toString(),
                "requestId", requestId.toString(),
                "action", "TIMELINE_READ",
                "outcome", "DENIED"
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(expected.toString()));
    }

    @Test
    void shouldApplyInclusiveFromAndExclusiveTo() throws Exception {
        seed(
                studentId, teacher.getId(), requestId,
                AuditAction.TIMELINE_READ, AuditOutcome.SUCCESS,
                "2026-01-10T08:00:00Z"
        );

        seed(
                studentId, teacher.getId(), requestId,
                AuditAction.TIMELINE_READ, AuditOutcome.SUCCESS,
                "2026-01-10T12:00:00Z"
        );

        seed(
                studentId,
                teacher.getId(),
                requestId,
                AuditAction.TIMELINE_READ,
                AuditOutcome.SUCCESS,
                "2026-01-10T18:00:00Z"
        );

        read(
                "requestId",
                requestId.toString(),
                "from",
                "2026-01-10T08:00:00Z",
                "to",
                "2026-01-10T18:00:00Z"
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void teacherWithAdminClaimShouldBeDeniedAndAttemptPreserved() throws Exception {
        var result = mockMvc.perform(
                        get("/api/audit-events")
                                .with(jwt()
                                        .jwt(token -> token.subject(teacher.getEmail()))
                                        .authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                .andExpect(status().isForbidden())
                .andReturn();

        UUID deniedRequestId = UUID.fromString(
                result.getResponse().getHeader("X-Request-ID")
        );

        UUID actorId = teacher.getId();

        TestTransaction.flagForRollback();
        TestTransaction.end();

        var events = auditRepository.findAllByRequestIdOrderByOccurredAtAscIdAsc(deniedRequestId);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getOutcome()).isEqualTo(AuditOutcome.DENIED);
        assertThat(events.getFirst().getActorUserId()).isEqualTo(actorId);
    }

    @Test
    void inactiveAdministratorShouldBeRejected() throws Exception {
        administrator.deactivate();
        userRepository.saveAndFlush(administrator);

        read().andExpect(status().isForbidden());
    }

    @Test
    void currentAdministratorShouldNotDependOnOldTokenRole() throws Exception {
        mockMvc.perform(get("/api/audit-events")
                        .param("requestId", requestId.toString())
                        .with(jwt().jwt(token -> token.subject(administrator.getEmail()))
                                .authorities(new SimpleGrantedAuthority("ROLE_TEACHER"))))
                .andExpect(status().isOk());
    }

    @Test
    void unknownIdentityShouldBeRejected() throws Exception {
        mockMvc.perform(
                get("/api/audit-events")
                        .with(jwt().jwt(token -> token.subject("missing@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidFiltersShouldBeRejected() throws Exception {
        read("page", "-1").andExpect(status().isBadRequest());
        read("size", "101").andExpect(status().isBadRequest());

        read("from", "2026-01-11T00:00:00Z", "to", "2026-01-10T00:00:00Z")
                .andExpect(status().isBadRequest());

        read("action", "INVALID_ACTION").andExpect(status().isBadRequest());

        read("studentId", "invalid-uuid").andExpect(status().isBadRequest());
    }

    @Test
    void anonymousRequestShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/audit-events")).andExpect(status().isUnauthorized());
    }

    @Test
    void mutationEndpointsShouldNotBeAvailable() throws Exception {
        mockMvc.perform(
                        post("/api/audit-events").with(jwt().jwt(token -> token.subject(
                                administrator.getEmail()))))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(
                        delete("/api/audit-events")
                                .with(jwt().jwt(token -> token.subject(administrator.getEmail()))))
                .andExpect(status().isMethodNotAllowed());
    }

    private ResultActions read(String... parameters) throws Exception {
        var request = get("/api/audit-events")
                .with(jwt().jwt(token -> token.subject(administrator.getEmail())));

        for (int index = 0; index < parameters.length; index += 2) {
            request.param(parameters[index], parameters[index + 1]);
        }

        return mockMvc.perform(request);
    }

    private UUID seed(
            UUID targetStudentId,
            UUID actorId,
            UUID targetRequestId,
            AuditAction action,
            AuditOutcome outcome,
            String occurredAt
    ) {
        UUID eventId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO audit_events (
                    id,
                    actor_user_id,
                    action,
                    outcome,
                    student_id,
                    resource_id,
                    request_id,
                    occurred_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                eventId,
                actorId,
                action.name(),
                outcome.name(),
                targetStudentId,
                null,
                targetRequestId,
                OffsetDateTime.parse(occurredAt)
        );
        return eventId;
    }
}