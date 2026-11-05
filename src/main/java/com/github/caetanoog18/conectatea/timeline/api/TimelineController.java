package com.github.caetanoog18.conectatea.timeline.api;

import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import com.github.caetanoog18.conectatea.timeline.api.dto.TimelineEventResponse;
import com.github.caetanoog18.conectatea.timeline.application.TimelineService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        name = "Linha do tempo",
        description = """
                Consulta cronológica dos registros autorizados do estudante.

                Atualmente a linha do tempo é formada por observações.
                O acesso depende do vínculo profissional e do
                consentimento válido no momento da consulta.
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
                        Profissional, estudante, vínculo ou consentimento
                        sem autorização válida
                        """,
                content = @Content
        )
})
@RestController
@RequestMapping("/api/me/students/{studentId}/timeline")
@PreAuthorize("isAuthenticated()")
public class TimelineController {
    private final TimelineService timelineService;
    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @Operation(
            operationId = "getAuthorizedStudentTimeline",
            summary = "Consultar linha do tempo do estudante",
            description = """
                Retorna eventos da finalidade autorizada para o
                profissional autenticado.

                O parâmetro from é inclusivo e o parâmetro to é exclusivo.
                Quando ambos são informados, from deve ser anterior a to.

                Os eventos são ordenados do mais recente para o mais antigo.
                Em caso de empate, o identificador é usado como critério
                determinístico.

                Não existe parâmetro purpose: a finalidade é determinada
                pelo perfil atual do profissional no banco.

                A consulta é registrada na auditoria.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de eventos autorizados",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "X-Request-ID",
                            description = "Identificador da consulta na auditoria",
                            schema = @Schema(type = "string", format = "uuid")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        Identificador, período, data ou paginação inválida
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
    public ResponseEntity<PagedResponse<TimelineEventResponse>> findByStudent(
            @PathVariable UUID studentId,


            @Parameter(
                    description = "Início inclusivo do período",
                    example = "2026-09-01T00:00:00Z"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @Parameter(
                    description = "Fim exclusivo do período",
                    example = "2026-10-01T00:00:00Z"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,

            @Parameter(description = "Índice da página, começando em zero")
            @Min(value = 0, message = "Page must be zero or greater")
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(description = "Quantidade de eventos, entre 1 e 100")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must be at most 100")
            int size,

            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(timelineService
                .findByStudent(studentId, from, to, page, size, jwt.getSubject()));
    }
}