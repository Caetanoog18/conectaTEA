package com.github.caetanoog18.conectatea.careteam.api;

import com.github.caetanoog18.conectatea.careteam.api.dto.AccessibleStudentResponse;
import com.github.caetanoog18.conectatea.careteam.api.dto.LegalGuardianContactResponse;
import com.github.caetanoog18.conectatea.careteam.application.StudentAccessService;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/students")
@PreAuthorize("""
        hasAnyRole(
            'TEACHER',
            'AEE_TEACHER',
            'PEDAGOGICAL_COORDINATOR',
            'PSYCHOLOGIST',
            'PHYSICIAN'
        )
        """)
@Tag(
        name = "Acesso profissional",
        description = "Consulta de estudantes e contatos sob autorização"
)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Autenticação ausente ou inválida"
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Acesso não autorizado"
        )
})
public class ProfessionalDirectoryController {
    private final StudentAccessService studentAccessService;

    public ProfessionalDirectoryController(StudentAccessService studentAccessService) {
        this.studentAccessService = studentAccessService;
    }

    @Operation(
            operationId = "listAccessibleStudents",
            summary = "Listar estudantes autorizados",
            description = """
                    Lista somente estudantes ativos com vínculo profissional
                    vigente e consentimento válido para a finalidade aplicável
                    ao perfil atual no banco e compartilhamento com a equipe.

                    A autorização é aplicada antes da paginação.
                    O total considera somente estudantes autorizados.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de estudantes autorizados",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Paginação inválida"
            )
    })
    @GetMapping
    public ResponseEntity<PagedResponse<AccessibleStudentResponse>> findAll(
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size,

            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(studentAccessService.findAccessibleStudents(page, size, jwt.getSubject()));
    }

    @Operation(
            operationId = "listAccessibleLegalGuardianContacts",
            summary = "Consultar contatos dos responsáveis legais",
            description = """
                    Exige acesso profissional vigente ao estudante.

                    Retorna somente responsáveis legais ativos cujo vínculo
                    possua consentimento válido com a finalidade aplicável
                    ao profissional e compartilhamento com a equipe.

                    Não retorna CPF nem identificador da conta do responsável.
                    Estudantes inexistentes ou inacessíveis resultam em 403.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Contatos autorizados",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador inválido"
            )
    })
    @GetMapping("/{studentId}/legal-guardians")
    public ResponseEntity<List<LegalGuardianContactResponse>> findContacts(
            @PathVariable UUID studentId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(studentAccessService.findLegalGuardianContacts(studentId, jwt.getSubject()));
    }
}