package com.github.caetanoog18.conectatea.careteam.api;

import com.github.caetanoog18.conectatea.careteam.api.dto.CreateProfessionalLinkRequest;
import com.github.caetanoog18.conectatea.careteam.api.dto.EndProfessionalLinkRequest;
import com.github.caetanoog18.conectatea.careteam.api.dto.ProfessionalLinkResponse;
import com.github.caetanoog18.conectatea.careteam.application.CareTeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
import java.util.List;
import java.util.UUID;


@Tag(
        name = "Equipe de cuidado",
        description = """
                Gerencia os vínculos entre estudantes e profissionais.

                As operações exigem uma conta ativa com perfil
                ADMINISTRATOR ou PEDAGOGICAL_COORDINATOR.

                O perfil e o status atuais do usuário são novamente
                verificados no banco de dados.
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
                        Usuário sem permissão, inativo ou cujo perfil
                        atual no banco não permite gerenciar a equipe
                        """,
                content = @Content
        )
})
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PEDAGOGICAL_COORDINATOR')")
public class CareTeamController {
    private final CareTeamService careTeamService;
    public CareTeamController(CareTeamService careTeamService) {
        this.careTeamService = careTeamService;
    }

    @Operation(
            operationId = "createProfessionalLink",
            summary = "Vincular profissional ao estudante",
            description = """
                Cria um vínculo ativo entre o estudante e o profissional.

                O estudante e o profissional devem estar ativos.
                A data inicial não pode estar no futuro.

                Perfis profissionais permitidos:
                PEDAGOGICAL_COORDINATOR, TEACHER, AEE_TEACHER,
                PSYCHOLOGIST e PHYSICIAN.

                Não pode existir outro vínculo ativo entre o mesmo
                estudante e profissional.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Vínculo profissional criado",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "Location",
                            description = "Endereço do vínculo criado",
                            schema = @Schema(type = "string", format = "uri")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador, corpo ou campos inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Estudante ou usuário profissional não encontrado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Já existe um vínculo ativo ou ocorreu conflito de dados",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = """
                        Estudante ou profissional inativo, perfil não elegível
                        ou data inicial no futuro
                        """,
                    content = @Content
            )
    })
    @PostMapping("/students/{studentId}/care-team")
    public ResponseEntity<ProfessionalLinkResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateProfessionalLinkRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProfessionalLinkResponse response = careTeamService.create(studentId, request, jwt.getSubject());

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/care-team-links/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }


    @Operation(
            operationId = "listStudentCareTeam",
            summary = "Listar equipe de cuidado do estudante",
            description = """
                Retorna vínculos ativos e encerrados, preservando
                o histórico da equipe de cuidado.

                Os resultados são ordenados pelo nome do profissional.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Equipe de cuidado encontrada",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Estudante não encontrado",
                    content = @Content
            )
    })
    @GetMapping("/students/{studentId}/care-team")
    public ResponseEntity<List<ProfessionalLinkResponse>> findByStudent(
            @PathVariable UUID studentId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(careTeamService.findByStudent(studentId, jwt.getSubject()));
    }

    @Operation(
            operationId = "getProfessionalLink",
            summary = "Consultar vínculo profissional"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vínculo encontrado",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Vínculo profissional não encontrado",
                    content = @Content
            )
    })
    @GetMapping("/care-team-links/{linkId}")
    public ResponseEntity<ProfessionalLinkResponse> findById(
            @PathVariable UUID linkId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(careTeamService.findById(linkId, jwt.getSubject()));
    }

    @Operation(
            operationId = "endProfessionalLink",
            summary = "Encerrar vínculo profissional",
            description = """
                Encerra um vínculo ativo sem excluir seu histórico.

                A data de encerramento é definida pelo servidor em UTC.
                Um vínculo encerrado não pode ser encerrado novamente.
                O profissional poderá ser vinculado novamente no futuro,
                criando um novo registro.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vínculo encerrado",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador, corpo ou motivo inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Vínculo profissional não encontrado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "O vínculo já está encerrado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Data de encerramento inconsistente",
                    content = @Content
            )
    })
    @PatchMapping("/care-team-links/{linkId}/end")
    public ResponseEntity<ProfessionalLinkResponse> end(
            @PathVariable UUID linkId,
            @Valid @RequestBody EndProfessionalLinkRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(careTeamService.end(linkId, request, jwt.getSubject()));
    }
}