package com.github.caetanoog18.conectatea.consent.api;

import com.github.caetanoog18.conectatea.consent.api.dto.ConsentResponse;
import com.github.caetanoog18.conectatea.consent.api.dto.CreateConsentRequest;
import com.github.caetanoog18.conectatea.consent.api.dto.RevokeConsentRequest;
import com.github.caetanoog18.conectatea.consent.application.ConsentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(
        name = "Consentimentos",
        description = """
                Registro, consulta e revogação dos consentimentos.

                Todas as operações exigem ADMINISTRATOR ou
                PEDAGOGICAL_COORDINATOR.
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
                description = "Perfil sem acesso aos consentimentos",
                content = @Content
        )
})
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PEDAGOGICAL_COORDINATOR')")
public class ConsentController {
    private final ConsentService consentService;
    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @Operation(
            operationId = "createConsent",
            summary = "Registrar consentimento",
            description = """
                Registra consentimento para um vínculo com responsável legal.

                Estudante e responsável devem estar ativos.
                Pode existir somente um consentimento ACTIVE por vínculo.

                Um consentimento vencido é marcado como EXPIRED
                antes da criação do novo termo.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Consentimento registrado",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "Location",
                            description = "Endereço do consentimento",
                            schema = @Schema(type = "string", format = "uri")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador, corpo, data ou campos inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "O vínculo já possui consentimento ativo",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = """
                        Vínculo inexistente, responsável não legal,
                        estudante inativo, responsável inativo ou
                        período de validade inconsistente
                        """,
                    content = @Content
            )
    })
    @PostMapping("/student-guardian-links/{linkId}/consents")
    public ResponseEntity<ConsentResponse> create(
            @PathVariable UUID linkId,
            @Valid @RequestBody CreateConsentRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        ConsentResponse response = consentService.create(linkId, request, jwt.getSubject());

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/consents/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            operationId = "getConsent",
            summary = "Consultar consentimento",
            description = """
                Retorna o consentimento pelo identificador.
                Se necessário, atualiza seu estado para EXPIRED.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Consentimento encontrado",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Consentimento não encontrado",
                    content = @Content
            )
    })
    @GetMapping("/consents/{consentId}")
    public ResponseEntity<ConsentResponse> findById(@PathVariable UUID consentId) {
        return ResponseEntity.ok(consentService.findById(consentId));
    }


    @Operation(
            operationId = "getActiveConsent",
            summary = "Consultar consentimento ativo do vínculo"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Consentimento ativo encontrado",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Consentimento ativo não encontrado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Vínculo com responsável não encontrado",
                    content = @Content
            )
    })
    @GetMapping("/student-guardian-links/{linkId}/consents/active")
    public ResponseEntity<ConsentResponse> findActive(@PathVariable UUID linkId) {
        return ResponseEntity.ok(consentService.findActive(linkId));
    }


    @Operation(
            operationId = "listConsentHistory",
            summary = "Consultar histórico de consentimentos",
            description = """
                Retorna todos os consentimentos do vínculo,
                ordenados da concessão mais recente para a mais antiga.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Histórico encontrado",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Vínculo com responsável não encontrado",
                    content = @Content
            )
    })
    @GetMapping("/student-guardian-links/{linkId}/consents")
    public ResponseEntity<List<ConsentResponse>> findHistory(
            @PathVariable UUID linkId
    ) {
        return ResponseEntity.ok(consentService.findHistory(linkId));
    }

    @Operation(
            operationId = "revokeConsent",
            summary = "Revogar consentimento",
            description = """
                Revoga um consentimento ACTIVE e registra o motivo,
                o instante e o usuário responsável pela revogação.

                Consentimentos vencidos ou já revogados não podem
                ser revogados novamente.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Consentimento revogado",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador, corpo ou motivo inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Consentimento não encontrado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Consentimento vencido ou não ativo",
                    content = @Content
            )
    })
    @PatchMapping("/consents/{consentId}/revoke")
    public ResponseEntity<ConsentResponse> revoke(
            @PathVariable UUID consentId,
            @Valid @RequestBody RevokeConsentRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt)
    {
        return ResponseEntity.ok(consentService.revoke(consentId, request, jwt.getSubject()));
    }
}