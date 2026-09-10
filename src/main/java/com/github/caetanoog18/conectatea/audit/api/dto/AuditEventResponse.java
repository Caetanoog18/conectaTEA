package com.github.caetanoog18.conectatea.audit.api.dto;

import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditEvent;
import com.github.caetanoog18.conectatea.audit.domain.AuditOutcome;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(
        description = """
                Evento imutável que registra uma operação sensível
                executada ou recusada pelo sistema.
                """
)
public record AuditEventResponse(
        @Schema(
                description = "Identificador do evento",
                format = "uuid"
        )
        UUID id,

        @Schema(
                description = """
                        Usuário identificado como autor da operação.
                        Pode ser nulo quando a identidade não foi localizada.
                        """,
                format = "uuid"
        )
        UUID actorUserId,


        @Schema(description = "Ação que estava sendo executada", example = "OBSERVATION_READ")
        AuditAction action,

        @Schema(description = "Resultado da operação", example = "SUCCESS")
        AuditOutcome outcome,

        @Schema(
                description = """
                        Estudante relacionado ao evento, quando aplicável
                        """,
                format = "uuid"
        )
        UUID studentId,

        @Schema(
                description = """
                        Recurso específico relacionado ao evento,
                        como uma observação
                        """,
                format = "uuid"
        )
        UUID resourceId,

        @Schema(
                description = """
                        Identificador de correlação da requisição.
                        Também é devolvido pelo cabeçalho X-Request-ID.
                        """,
                format = "uuid"
        )
        UUID requestId,

        @Schema(
                description = "Momento que o evento foi registrado",
                type = "string",
                format = "date-time",
                example = "2026-09-10T12:00:00Z"
        )
        Instant occurredAt
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActorUserId(),
                event.getAction(),
                event.getOutcome(),
                event.getStudentId(),
                event.getResourceId(),
                event.getRequestId(),
                event.getOccurredAt()
        );
    }
}