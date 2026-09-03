package com.github.caetanoog18.conectatea.student.api;

import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import com.github.caetanoog18.conectatea.student.api.dto.StudentRequest;
import com.github.caetanoog18.conectatea.student.api.dto.StudentResponse;
import com.github.caetanoog18.conectatea.student.api.dto.UpdateStudentStatusRequest;
import com.github.caetanoog18.conectatea.student.application.StudentService;
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
        name = "Estudantes - administração",
        description = """
                Gerenciamento administrativo de estudantes.
                Exige ADMINISTRATOR ou PEDAGOGICAL_COORDINATOR no JWT.

                Não confundir com /api/me/students, que possui regras
                próprias de vínculo profissional e consentimento.
                """
)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Token ausente, inválido ou expirado"
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Perfil sem acesso ao gerenciamento de estudantes"

        )
})
@RestController
@RequestMapping("/api/students")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PEDAGOGICAL_COORDINATOR')")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @Operation(
            operationId = "createStudent",
            summary = "Cadastrar estudante",
            description = """
                Cria um estudante ativo.
                A matrícula deve ser única, desconsiderando maiúsculas e minúsculas.
                Não cria automaticamente responsáveis, consentimentos
                ou vínculos profissionais.
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Estudante criado",
                            useReturnTypeSchema = true,
                            headers = @Header(
                                    name = "Location",
                                    description = "Endereço do estudante criado",
                                    schema = @Schema(type = "string", format = "uri")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Dados de cadastro inválidos"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Matrícula já utilizada"
                    )
            }
    )
    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            operationId = "listStudents",
            summary = "Listar estudantes",
            description = """
                Lista estudantes ativos e inativos, ordenados por fullName crescente.
                A primeira página é zero.
                Não há filtro de status nem parâmetro de ordenação neste endpoint.
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Página de estudantes",
                            useReturnTypeSchema = true

                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Parâmetros de paginação inválidos"

                    )
            }
    )
    @GetMapping
    public ResponseEntity<PagedResponse<StudentResponse>> findAll(
            @Parameter(description = "Página solicitada, começando em zero")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @Parameter(description = "Quantidade por página: de 1 a 100")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must be at most 100")
            int size
    ) {
        return ResponseEntity.ok(studentService.findAll(page, size));
    }


    @Operation(
            operationId = "getStudent",
            summary = "Consultar estudante pelo identificador",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Estudante encontrado",
                            useReturnTypeSchema = true

                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Identificador com formato inválido"

                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Estudante não encontrado"

                    )
            }
    )
    @GetMapping("/{studentId}")
    public ResponseEntity<StudentResponse> findById(@PathVariable UUID studentId) {
        return ResponseEntity.ok(studentService.findById(studentId));
    }


    @Operation(
            operationId = "updateStudent",
            summary = "Atualizar o cadastro do estudante",
            description = """
                Envie todos os campos obrigatórios de StudentRequest.
                O status ativo/inativo é alterado em uma operação separada.
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Estudante atualizado",
                            useReturnTypeSchema = true

                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Identificador ou dados inválidos"

                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Estudante não encontrado"

                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Matrícula utilizada por outro estudante"

                    )
            }
    )
    @PutMapping("/{studentId}")
    public ResponseEntity<StudentResponse> update(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentRequest request
    ) {
        return ResponseEntity.ok(studentService.update(studentId, request));
    }


    @Operation(
            operationId = "updateStudentStatus",
            summary = "Ativar ou desativar estudante",
            description = """
                Recebe active=true para ativar ou active=false para desativar.
                O campo é obrigatório.
                A desativação não exclui o cadastro nem seu histórico.
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Status atualizado",
                            useReturnTypeSchema = true

                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Identificador ou corpo inválido"

                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Estudante não encontrado"
                    )
            }
    )
    @PatchMapping("/{studentId}/status")
    public ResponseEntity<StudentResponse> updateStatus(
            @PathVariable UUID studentId,
            @Valid @RequestBody UpdateStudentStatusRequest request
    ) {
        return ResponseEntity.ok(studentService.updateStatus(studentId, request));
    }
}