package com.github.caetanoog18.conectatea.guardian.api;

import com.github.caetanoog18.conectatea.guardian.api.dto.CreateStudentGuardianLinkRequest;
import com.github.caetanoog18.conectatea.guardian.api.dto.StudentGuardianResponse;
import com.github.caetanoog18.conectatea.guardian.api.dto.UpdateStudentGuardianLinkRequest;
import com.github.caetanoog18.conectatea.guardian.application.StudentGuardianService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;


@Tag(
        name = "Vínculos com responsáveis",
        description = """
                Gerencia os vínculos entre estudantes e responsáveis.

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
                description = "Perfil sem acesso ao gerenciamento dos vínculos",
                content = @Content
        )
})
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PEDAGOGICAL_COORDINATOR')")
public class StudentGuardianController {
    private final StudentGuardianService service;
    public StudentGuardianController(StudentGuardianService service) {
        this.service = service;
    }


    @Operation(
            operationId = "createStudentGuardianLink",
            summary = "Vincular responsável ao estudante",
            description = """
                O estudante e o responsável devem estar ativos.

                Um responsável não pode ser vinculado duas vezes
                ao mesmo estudante. Cada estudante pode ter apenas
                um vínculo marcado como contato principal.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Vínculo criado",
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
                    description = "Estudante ou responsável não encontrado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Vínculo duplicado ou contato principal já existente",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Estudante ou responsável inativo",
                    content = @Content
            )
    })
    @PostMapping("/students/{studentId}/guardians")
    public ResponseEntity<StudentGuardianResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody
            CreateStudentGuardianLinkRequest request
    ) {
        StudentGuardianResponse response = service.create(studentId, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{guardianId}")
                .buildAndExpand(response.guardianId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            operationId = "listStudentGuardians",
            summary = "Listar os responsáveis do estudante"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vínculos encontrados",
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
    @GetMapping("/students/{studentId}/guardians")
    public ResponseEntity<List<StudentGuardianResponse>> findByStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(service.findByStudent(studentId));
    }


    @Operation(
            operationId = "listGuardianStudents",
            summary = "Listar estudantes de determinado responsável"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vínculos encontrados",
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
    @GetMapping("/guardians/{guardianId}/students")
    public ResponseEntity<List<StudentGuardianResponse>> findByGuardian(@PathVariable UUID guardianId) {
        return ResponseEntity.ok(service.findByGuardian(guardianId));
    }


    @Operation(
            operationId = "updateStudentGuardianLink",
            summary = "Atualizar vínculo com responsável",
            description = """
                Atualiza parentesco, a condição de responsável legal
                e a definição de contato principal.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vínculo atualizado",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador, corpo ou campos inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Vínculo não encontrado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Outro contato principal já existe ou houve conflito de dados",
                    content = @Content
            )
    })
    @PutMapping("/students/{studentId}/guardians/{guardianId}")
    public ResponseEntity<StudentGuardianResponse> update(
            @PathVariable UUID studentId,
            @PathVariable UUID guardianId,
            @Valid @RequestBody
            UpdateStudentGuardianLinkRequest request
    ) {
        return ResponseEntity.ok(service.update(studentId, guardianId, request));
    }

    @Operation(
            operationId = "deleteStudentGuardianLink",
            summary = "Deleta vínculo com responsável",
            description = """
                Deleta o vínculo identificado pelo estudante e responsável.

                Esta operação não deve ser usada para apagar vínculos
                que possuam consentimentos ou histórico relacionado.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Vínculo excluído",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Vínculo não encontrado",
                    content = @Content
            )
    })
    @DeleteMapping("/students/{studentId}/guardians/{guardianId}")
    public ResponseEntity<Void> delete(@PathVariable UUID studentId, @PathVariable UUID guardianId) {
        service.delete(studentId, guardianId);
        return ResponseEntity.noContent().build();
    }
}