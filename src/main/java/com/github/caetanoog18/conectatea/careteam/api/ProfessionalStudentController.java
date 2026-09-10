package com.github.caetanoog18.conectatea.careteam.api;

import com.github.caetanoog18.conectatea.careteam.application.StudentAccessService;
import com.github.caetanoog18.conectatea.student.api.dto.StudentResponse;
import com.github.caetanoog18.conectatea.student.domain.Student;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;


@Tag(
        name = "Acesso profissional",
        description = """
                Acesso do profissional ao perfil de um estudante.

                A autorização considera simultaneamente:
                conta profissional ativa, estudante ativo, vínculo
                profissional vigente e consentimento válido.
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
                        Acesso não autorizado. A resposta também é usada
                        quando o estudante não existe ou está inacessível,
                        evitando revelar a existência do cadastro.
                        """,
                content = @Content
        )
})
@RestController
@RequestMapping("/api/me/students")
@PreAuthorize("hasAnyRole('TEACHER', 'AEE_TEACHER', " + "'PEDAGOGICAL_COORDINATOR', 'PSYCHOLOGIST', 'PHYSICIAN')")
public class ProfessionalStudentController {
    private final StudentAccessService studentAccessService;

    public ProfessionalStudentController(StudentAccessService studentAccessService) {
        this.studentAccessService = studentAccessService;
    }


    @Operation(
            operationId = "getAccessibleStudentProfile",
            summary = "Consultar perfil autorizado do estudante",
            description = """
                TEACHER e AEE_TEACHER precisam de um consentimento
                válido com EDUCATIONAL_SUPPORT e
                INFORMATION_SHARING_WITH_CARE_TEAM.

                PEDAGOGICAL_COORDINATOR, PSYCHOLOGIST e PHYSICIAN
                precisam de MULTIPROFESSIONAL_MONITORING e
                INFORMATION_SHARING_WITH_CARE_TEAM.

                As duas finalidades precisam pertencer ao mesmo termo
                de consentimento válido.

                O acesso é negado imediatamente se a conta, o estudante,
                o responsável legal, o vínculo ou o consentimento deixar
                de ser válido.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil autorizado do estudante",
                    useReturnTypeSchema = true
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador inválido",
                    content = @Content
            )
    })
    @GetMapping("/{studentId}")
    public ResponseEntity<StudentResponse> findById(
            @PathVariable UUID studentId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        Student student = studentAccessService.requireProfileReadAccess(studentId, jwt.getSubject());

        return ResponseEntity.ok(StudentResponse.from(student));
    }
}