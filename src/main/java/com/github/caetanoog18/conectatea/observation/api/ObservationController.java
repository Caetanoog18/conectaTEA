package com.github.caetanoog18.conectatea.observation.api;

import com.github.caetanoog18.conectatea.observation.api.dto.CreateObservationRequest;
import com.github.caetanoog18.conectatea.observation.api.dto.ObservationResponse;
import com.github.caetanoog18.conectatea.observation.application.ObservationService;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;

@Tag(
        name = "Observações",
        description = """
                Registros profissionais relacionados aos estudantes.

                O acesso exige conta ativa, estudante ativo, vínculo
                profissional vigente e consentimento válido.

                A finalidade da observação é determinada pelo perfil
                atual do profissional no banco de dados.
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
                        Acesso não autorizado. Esta resposta também evita
                        revelar a existência de estudantes inacessíveis.
                        """,
                content = @Content
        )
})
@RestController
@RequestMapping("/api/me/students/{studentId}/observations")
@PreAuthorize("isAuthenticated()")
public class ObservationController {
    private final ObservationService observationService;

    public ObservationController(ObservationService observationService) {
        this.observationService = observationService;
    }


    @Operation(
            operationId = "createObservation",
            summary = "Registrar observação",
            description = """
                Registra uma observação em nome do profissional autenticado.

                TEACHER e AEE_TEACHER geram observações com finalidade
                EDUCATIONAL_SUPPORT.

                PEDAGOGICAL_COORDINATOR, PSYCHOLOGIST e PHYSICIAN
                geram observações com finalidade
                MULTIPROFESSIONAL_MONITORING.

                A finalidade não é recebida no corpo da requisição.
                A operação é registrada na auditoria.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Observação registrada",
                    useReturnTypeSchema = true,
                    headers = {
                            @Header(
                                    name = "Location",
                                    description = "Endereço da observação criada",
                                    schema = @Schema(type = "string", format = "uri")
                            ),
                            @Header(
                                    name = "X-Request-ID",
                                    description = "Identificador da operação na auditoria",
                                    schema = @Schema(type = "string", format = "uuid")
                            )
                    }
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador, corpo ou campos inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "A auditoria não pôde ser persistida",
                    content = @Content
            )
    })
    @PostMapping
    public ResponseEntity<ObservationResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateObservationRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        ObservationResponse response = observationService.create(studentId, request, jwt.getSubject());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            operationId = "listAuthorizedObservations",
            summary = "Listar observações autorizadas",
            description = """
                Retorna apenas observações do estudante e da finalidade
                autorizada para o perfil atual do profissional.

                Os registros são ordenados por occurredAt decrescente.
                Em caso de empate, o identificador é usado como critério
                determinístico.

                A consulta é registrada na auditoria.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de observações autorizadas",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "X-Request-ID",
                            description = "Identificador da consulta na auditoria",
                            schema = @Schema(type = "string", format = "uuid")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador ou paginação inválida",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "A auditoria não pôde ser persistida",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<PagedResponse<ObservationResponse>> findAll(
            @PathVariable UUID studentId,

            @Parameter(description = "Índice da página, começando em zero")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @Parameter(description = "Quantidade de registros, entre 1 e 100")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must be at most 100")
            int size,

            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(observationService.findAll(studentId, page, size, jwt.getSubject()));
    }

    @Operation(
            operationId = "getAuthorizedObservation",
            summary = "Consultar observação autorizada",
            description = """
                Retorna a observação somente quando ela pertence ao
                estudante e à finalidade autorizada para o profissional.

                Uma observação de outro estudante ou de outra finalidade
                é apresentada como não encontrada.

                A consulta é registrada na auditoria.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Observação encontrada",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "X-Request-ID",
                            description = "Identificador da consulta na auditoria",
                            schema = @Schema(type = "string", format = "uuid")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                        Observação não encontrada no estudante
                        e na finalidade autorizada
                        """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "A auditoria não pôde ser persistida",
                    content = @Content
            )
    })
    @GetMapping("/{observationId}")
    public ResponseEntity<ObservationResponse> findById(
            @PathVariable UUID studentId,
            @PathVariable UUID observationId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(observationService.findById(studentId, observationId, jwt.getSubject()));
    }
}