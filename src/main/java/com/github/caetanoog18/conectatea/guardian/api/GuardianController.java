package com.github.caetanoog18.conectatea.guardian.api;

import com.github.caetanoog18.conectatea.guardian.api.dto.GuardianRequest;
import com.github.caetanoog18.conectatea.guardian.api.dto.GuardianResponse;
import com.github.caetanoog18.conectatea.guardian.api.dto.UpdateGuardianStatusRequest;
import com.github.caetanoog18.conectatea.guardian.application.GuardianService;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import java.util.UUID;


@Tag(
        name = "Responsáveis",
        description = """
                Cadastro administrativo de responsáveis.

                Todas as operações exigem os perfis
                ADMINISTRATOR ou PEDAGOGICAL_COORDINATOR.
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
                description = "Perfil Nao tem acesso ao gerenciamento de responsáveis",
                content = @Content
        )
})
@RestController
@RequestMapping("/api/guardians")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PEDAGOGICAL_COORDINATOR')")
public class GuardianController {
    private final GuardianService guardianService;
    public GuardianController(GuardianService guardianService) {
        this.guardianService = guardianService;
    }


    @Operation(
            operationId = "createGuardian",
            summary = "Cadastrar um responsável",
            description = """
                Cadastra um responsável.

                O CPF é opcional, mas, quando informado, deve
                ser válido e não pode pertencer a outro responsável registrado.

                O userId é opcional. Quando informado, precisa
                identificar um usuário com perfil LEGAL_GUARDIAN
                que ainda não esteja associado a outro responsável.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Responsável cadastrado",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "Location",
                            description = "Endereço do responsável cadastrado",
                            schema = @Schema(type = "string", format = "uri")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Corpo ou campos inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "CPF, usuário ou dados já utilizados",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "userId não pertence a um usuário LEGAL_GUARDIAN",
                    content = @Content
            )
    })
    @PostMapping
    public ResponseEntity<GuardianResponse> create(@Valid @RequestBody GuardianRequest request) {
        GuardianResponse response = guardianService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }


    @Operation(
            operationId = "listGuardians",
            summary = "Listar responsáveis",
            description = """
                Retorna responsáveis ativos e inativos,
                ordenados pelo nome completo.
                A primeira página possui índice zero.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de responsáveis",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Paginação inválida",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<PagedResponse<GuardianResponse>> findAll(
            @Parameter(description = "Indice da pagina, começa em zero")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @Parameter(description = "Quantidade de registros, deve ser entre 1 e 100")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must be at most 100")
            int size
    ) {
        return ResponseEntity.ok(guardianService.findAll(page, size));
    }


    @Operation(
            operationId = "getGuardian",
            summary = "Consultar responsável"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Responsável encontrado",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Responsável não encontrado",
                    content = @Content
            )
    })
    @GetMapping("/{guardianId}")
    public ResponseEntity<GuardianResponse> findById(
            @PathVariable UUID guardianId
    ) {
        return ResponseEntity.ok(
                guardianService.findById(guardianId)
        );
    }


    @Operation(
            operationId = "updateGuardian",
            summary = "Atualizar responsável",
            description = """
                Substitui os dados cadastrais do responsável.
                O status ativo não é alterado nesta operação.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Responsável atualizado",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador, corpo ou campos inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Responsável não encontrado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "CPF, usuário ou dados já existentes",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Usuário associado inválido",
                    content = @Content
            )
    })
    @PutMapping("/{guardianId}")
    public ResponseEntity<GuardianResponse> update(
            @PathVariable UUID guardianId,
            @Valid @RequestBody GuardianRequest request
    ) {
        return ResponseEntity.ok(
                guardianService.update(guardianId, request)
        );
    }

    @Operation(
            operationId = "updateGuardianStatus",
            summary = "Ativar ou desativar responsável",
            description = """
                Recebe active=true ou active=false.
                A desativação preserva o cadastro e o histórico.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status atualizado",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador ou corpo inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Responsável não encontrado",
                    content = @Content
            )
    })
    @PatchMapping("/{guardianId}/status")
    public ResponseEntity<GuardianResponse> updateStatus(
            @PathVariable UUID guardianId,
            @Valid @RequestBody UpdateGuardianStatusRequest request
    ) {
        return ResponseEntity.ok(guardianService.updateStatus(guardianId, request));
    }
}