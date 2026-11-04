package com.github.caetanoog18.conectatea.medicaldocument.api;

import com.github.caetanoog18.conectatea.medicaldocument.api.dto.MedicalDocumentResponse;
import com.github.caetanoog18.conectatea.medicaldocument.application.MedicalDocumentService;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Tag(
        name = "Laudos médicos",
        description = "PDFs enviados por médicos e consultados pela equipe autorizada"
)
@RestController
@RequestMapping("/api/me/students/{studentId}/medical-documents")
@PreAuthorize("isAuthenticated()")
public class MedicalDocumentController {
    private final MedicalDocumentService service;

    public MedicalDocumentController(MedicalDocumentService service) {
        this.service = service;
    }

    @Operation(
            operationId = "uploadMedicalDocument",
            summary = "Enviar laudo médico em PDF"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Laudo armazenado",
            useReturnTypeSchema = true
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MedicalDocumentResponse> upload(
            @PathVariable UUID studentId,
            @RequestParam String title,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate issuedOn,
            @RequestPart MultipartFile file,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(service.upload(
                        studentId,
                        title,
                        issuedOn,
                        file,
                        jwt.getSubject()
                ));
    }

    @Operation(
            operationId = "listMedicalDocuments",
            summary = "Listar laudos autorizados sem os arquivos binários"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de laudos",
            useReturnTypeSchema = true
    )
    @GetMapping
    public ResponseEntity<PagedResponse<MedicalDocumentResponse>> findAll(
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.findAll(studentId, page, size, jwt.getSubject()));
    }

    @Operation(
            operationId = "downloadMedicalDocument",
            summary = "Baixar PDF após revalidar o acesso"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Arquivo PDF"
    )
    @GetMapping(value = "/{documentId}/file", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(
            @PathVariable UUID studentId,
            @PathVariable UUID documentId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        byte[] content = service.download(studentId, documentId, jwt.getSubject());

        String disposition = ContentDisposition.attachment()
                .filename("laudo-" + documentId + ".pdf")
                .build()
                .toString();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(content.length)
                .body(content);
    }
}