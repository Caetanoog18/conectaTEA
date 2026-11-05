package com.github.caetanoog18.conectatea.audit.api;

import com.github.caetanoog18.conectatea.audit.api.dto.AuditEventResponse;
import com.github.caetanoog18.conectatea.audit.application.AuditQueryService;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.audit.domain.AuditEventFilter;
import com.github.caetanoog18.conectatea.audit.domain.AuditOutcome;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Instant;
import java.util.UUID;


@Tag(
        name = "Auditoria",
        description = """
                Consulta administrativa dos eventos de auditoria.

                Somente uma conta ativa cujo perfil atual no banco seja
                ADMINISTRATOR pode acessar estes registros.

                A própria consulta também gera um evento de auditoria.
                """
)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Token ausente, inválido ou expirado",
                content = @Content
        ),
        @ApiResponse(
                responseCode = "403",
                description = """
                        Conta inativa, inexistente ou sem o perfil
                        ADMINISTRATOR no banco de dados
                        """,
                content = @Content
        )
})
@RestController
@RequestMapping("/api/audit-events")
@PreAuthorize("isAuthenticated()")
public class AuditController {
    private final AuditQueryService auditQueryService;
    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @Operation(
            operationId = "searchAuditEvents",
            summary = "Consultar eventos de auditoria",
            description = """
                Todos os filtros são opcionais e combinados com AND.

                O início do período, from, é inclusivo.
                O fim do período, to, é exclusivo.
                Quando ambos forem informados, from deve ser anterior a to.

                Os eventos são ordenados por occurredAt decrescente.
                Em caso de empate, o identificador é utilizado como
                critério determinístico.

                A consulta gera um novo evento AUDIT_EVENTS_LIST.
                Esse novo evento não aparece na própria resposta porque
                é persistido depois que a página consultada é construída.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de eventos de auditoria",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "X-Request-ID",
                            description = """
                                Identificador da própria consulta
                                registrada na auditoria
                                """,
                            schema = @Schema(
                                    type = "string",
                                    format = "uuid"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        UUID, enum, data, período ou paginação inválida
                        """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "A auditoria não pôde ser persistida",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<PagedResponse<AuditEventResponse>> search(
            @Parameter(
                    description = "Filtra eventos relacionados a um estudante",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @RequestParam(required = false)
            UUID studentId,

            @Parameter(
                    description = "Filtra eventos realizados por um usuário",
                    example = "550e8400-e29b-41d4-a716-446655440001"
            )
            @RequestParam(required = false)
            UUID actorUserId,

            @Parameter(
                    description = """
                            Filtra eventos pertencentes à mesma requisição.
                            Utilize o valor recebido no cabeçalho X-Request-ID.
                            """,
                    example = "550e8400-e29b-41d4-a716-446655440002"
            )
            @RequestParam(required = false)
            UUID requestId,

            @Parameter(description = "Filtra pela ação registrada")
            @RequestParam(required = false)
            AuditAction action,

            @Parameter(
                    description = "Filtra pelo resultado: SUCCESS, DENIED ou FAILURE ")
            @RequestParam(required = false)
            AuditOutcome outcome,

            @Parameter(description = "Início inclusivo do período", example = "2026-09-01T00:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @Parameter(description = "Fim exclusivo do período", example = "2026-10-01T00:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,

            @Parameter(description = "Índice da página, começando em zero")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @Parameter(description = "Quantidade de eventos, entre 1 e 100")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must be at most 100")
            int size,

            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuditEventFilter filter = new AuditEventFilter(
                studentId,
                actorUserId,
                requestId,
                action,
                outcome,
                from,
                to
        );

        return ResponseEntity.ok(auditQueryService.search(
                filter,
                page,
                size,
                jwt.getSubject())
        );
    }
}