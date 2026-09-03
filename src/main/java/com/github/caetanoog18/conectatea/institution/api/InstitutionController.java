package com.github.caetanoog18.conectatea.institution.api;

import com.github.caetanoog18.conectatea.institution.api.dto.InstitutionRequest;
import com.github.caetanoog18.conectatea.institution.api.dto.InstitutionResponse;
import com.github.caetanoog18.conectatea.institution.application.InstitutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;


@Tag(
        name = "Instituição",
        description = """
                Cadastro da única instituição desta instalação.
                A consulta exige autenticação.
                Criação e atualização exigem ADMINISTRATOR no JWT.
                """
)
@ApiResponse(
        responseCode = "401",
        description = "Token ausente, inválido ou expirado"
)
@RestController
@RequestMapping("/api/institution")
public class InstitutionController {
    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @Operation(
            operationId = "createInstitution",
            summary = "Cadastrar a instituição",
            description = "Permitido somente quando ainda não existe instituição cadastrada.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Instituição criada",
                            useReturnTypeSchema = true,
                            headers = @Header(
                                    name = "Location",
                                    description = "Endereço do cadastro da instituição",
                                    schema = @Schema(type = "string", format = "uri")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Dados inválidos"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Exige ADMINISTRATOR"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Já existe uma instituição cadastrada"
                    )
            }
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<InstitutionResponse> create(@Valid @RequestBody InstitutionRequest request) {
        InstitutionResponse response = institutionService.create(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            operationId = "getInstitution",
            summary = "Consultar a instituição",
            description = "Disponível para qualquer usuário autenticado.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Dados da instituição",
                            useReturnTypeSchema = true
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Instituição ainda não cadastrada"

                    )
            }
    )
    @GetMapping
    public ResponseEntity<InstitutionResponse> find() {
        return ResponseEntity.ok(institutionService.find());
    }

    @Operation(
            operationId = "updateInstitution",
            summary = "Atualizar a instituição",
            description = """
                Atualiza o cadastro existente.
                Envie todos os campos obrigatórios de InstitutionRequest.
                Esta operação não cria a instituição quando ela não existe.
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Instituição atualizada",
                            useReturnTypeSchema = true
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Dados inválidos"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Exige ADMINISTRATOR"

                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Instituição ainda não cadastrada"
                    )
            }
    )
    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<InstitutionResponse> update(@Valid @RequestBody InstitutionRequest request) {
        return ResponseEntity.ok(institutionService.update(request));
    }
}