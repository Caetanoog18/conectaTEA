package com.github.caetanoog18.conectatea.report.api;

import com.github.caetanoog18.conectatea.report.api.dto.GenerateStudentReportRequest;
import com.github.caetanoog18.conectatea.report.api.dto.StudentReportResponse;
import com.github.caetanoog18.conectatea.report.application.StudentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me/students/{studentId}/reports")
@PreAuthorize("isAuthenticated()")
@Tag(
        name = "Relatórios",
        description = "Geração de relatórios sob autorização e consentimento"
)
public class StudentReportController {
    private static final String ACCESS_RULES = """
            Exige usuário ativo, estudante ativo e vínculo profissional vigente.

            A finalidade é determinada pelo perfil atual no banco:
            TEACHER e AEE_TEACHER usam EDUCATIONAL_SUPPORT;
            PEDAGOGICAL_COORDINATOR, PSYCHOLOGIST e PHYSICIAN usam
            MULTIPROFESSIONAL_MONITORING.

            Um mesmo termo válido, associado a responsável legal ativo,
            deve incluir a finalidade aplicável,
            INFORMATION_SHARING_WITH_CARE_TEAM e REPORT_GENERATION.

            ADMINISTRATOR não possui acesso automático.

            São incluídas somente observações do estudante e da finalidade
            autorizada, no período solicitado, em ordem cronológica.

            Limites: 365 dias e 500 observações.
            Resultados maiores não são truncados, logo a operação é rejeitada.

            O conteúdo não é armazenado para recuperação posterior.
            Uma nova requisição realiza uma nova verificação de autorização.
            """;

    private final StudentReportService studentReportService;

    public StudentReportController(StudentReportService studentReportService) {
        this.studentReportService = studentReportService;
    }

    @Operation(
            operationId = "generateStudentReport",
            summary = "Gerar relatório em JSON",
            description = ACCESS_RULES + """
                    Registra a operação como REPORT_GENERATE na auditoria.
                    reportId identifica a geração, não uma URL de consulta.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Relatório gerado, inclusive se não houver observações",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = StudentReportResponse.class
                            )
                    ),
                    headers = {
                            @Header(
                                    name = "X-Request-ID",
                                    description = "Identificador para correlação na auditoria",
                                    schema = @Schema(type = "string", format = "uuid")
                            ),
                            @Header(
                                    name = "Cache-Control",
                                    description = "Resposta não deve ser armazenada em cache",
                                    schema = @Schema(type = "string", example = "no-store")
                            )
                    }
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "JSON, identificador ou período inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente, inválido ou expirado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = """
                            Acesso não autorizado: perfil, usuário, estudante,
                            vínculo ou consentimento não satisfaz as regras.
                            Estudante inexistente também é tratado como inacessível.
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "O período contém mais de 500 observações autorizadas",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Não foi possível persistir a auditoria de uma falha",
                    content = @Content
            )
    })
    @PostMapping
    public ResponseEntity<StudentReportResponse> generate(
            @Parameter(description = "Identificador do estudante")
            @PathVariable UUID studentId,

            @Valid @RequestBody GenerateStudentReportRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        var report = studentReportService.generate(studentId, request, jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(report);
    }

    @Operation(
            operationId = "exportStudentReportPdf",
            summary = "Exportar relatório em PDF",
            description = ACCESS_RULES + """
                    Retorna os bytes do arquivo, não JSON nem Base64.

                    Registra REPORT_PDF_EXPORT na auditoria.
                    O sucesso é registrado depois da renderização.
                    Isso não comprova que o cliente terminou o download.

                    A revogação bloqueia novas exportações, mas não remove
                    cópias já baixadas. O documento não é um laudo clínico !
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Arquivo PDF gerado para download",
                    content = @Content(
                            mediaType = "application/pdf",
                            schema = @Schema(type = "string", format = "binary")
                    ),
                    headers = {
                            @Header(
                                    name = "Content-Disposition",
                                    description = "Download como attachment, com nome do arquivo",
                                    schema = @Schema(type = "string")
                            ),
                            @Header(
                                    name = "X-Report-ID",
                                    description = "Identificador da geração",
                                    schema = @Schema(type = "string", format = "uuid")
                            ),
                            @Header(
                                    name = "X-Request-ID",
                                    description = "Identificador para correlação na auditoria",
                                    schema = @Schema(type = "string", format = "uuid")
                            ),
                            @Header(
                                    name = "Cache-Control",
                                    description = "Resposta não deve ser armazenada em cache",
                                    schema = @Schema(type = "string", example = "no-store")
                            )
                    }
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "JSON, identificador ou período inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente, inválido ou expirado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Os requisitos para o acesso do relatório não foram atendidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = """
                            Mais de 500 observações autorizadas ou presença
                            de caracteres não suportados pela fonte do PDF.
                            """,
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Falha interna na geração do PDF",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Não foi possível persistir a auditoria de uma falha",
                    content = @Content
            )
    })
    @PostMapping("/pdf")
    public ResponseEntity<byte[]> generatePdf(
            @Parameter(description = "Identificador do estudante")
            @PathVariable UUID studentId,

            @Valid @RequestBody GenerateStudentReportRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        var generated = studentReportService.generatePdf(studentId, request, jwt.getSubject());

        String filename = "conectatea-report-" + generated.reportId() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .header("X-Report-ID", generated.reportId().toString())
                .contentLength(generated.content().length)
                .body(generated.content());
    }
}