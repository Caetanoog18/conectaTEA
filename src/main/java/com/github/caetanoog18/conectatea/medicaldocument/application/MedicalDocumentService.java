package com.github.caetanoog18.conectatea.medicaldocument.application;

import com.github.caetanoog18.conectatea.audit.application.AuditedOperation;
import com.github.caetanoog18.conectatea.audit.domain.AuditAction;
import com.github.caetanoog18.conectatea.careteam.application.StudentAccessService;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.medicaldocument.api.dto.MedicalDocumentResponse;
import com.github.caetanoog18.conectatea.medicaldocument.domain.MedicalDocument;
import com.github.caetanoog18.conectatea.medicaldocument.infrastructure.MedicalDocumentRepository;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class MedicalDocumentService {
    private final MedicalDocumentRepository repository;
    private final StudentAccessService access;
    private final UserRepository users;
    private final AuditedOperation audit;
    private final PdfUploadValidator validator;
    private final Clock clock;

    public MedicalDocumentService(
            MedicalDocumentRepository repository,
            StudentAccessService access,
            UserRepository users,
            AuditedOperation audit,
            PdfUploadValidator validator,
            Clock clock
    ) {
        this.repository = repository;
        this.access = access;
        this.users = users;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public MedicalDocumentResponse upload(
            UUID studentId,
            String title,
            LocalDate issuedOn,
            MultipartFile file,
            String email
    ) {
        return audit.execute(
                AuditAction.MEDICAL_DOCUMENT_UPLOAD,
                studentId,
                null,
                email,
                () -> {
                    var physician = users.findByEmailIgnoreCase(email)
                            .filter(user ->
                                    user.isActive() &&
                                            user.getRole() == UserRole.PHYSICIAN
                            )
                            .orElseThrow(() -> new AccessDeniedException(
                                    "Only active physicians can upload medical documents"
                            ));

                    var student = access.requireProfileReadAccess(studentId, email);
                    String normalizedTitle = title == null ? "" : title.trim();

                    if (normalizedTitle.isBlank() || normalizedTitle.length() > 120) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Informe um título de até 120 caracteres."
                        );
                    }

                    LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("America/Sao_Paulo")));

                    if (issuedOn == null || issuedOn.isAfter(today)) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Informe uma data de emissão válida, sem data futura."
                        );
                    }

                    byte[] content = validator.validate(file);

                    var document = new MedicalDocument(student, physician, normalizedTitle, issuedOn, content);

                    return MedicalDocumentResponse.from(repository.saveAndFlush(document));
                },
                MedicalDocumentResponse::id
        );
    }

    public PagedResponse<MedicalDocumentResponse> findAll(UUID studentId, int page, int size, String email) {
        return audit.execute(
                AuditAction.MEDICAL_DOCUMENT_LIST,
                studentId,
                null,
                email,
                () -> {
                    access.requireProfileReadAccess(studentId, email);

                    if (page < 0 || size < 1 || size > 100) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Paginação inválida."
                        );
                    }

                    var pageable = PageRequest.of(
                            page,
                            size,
                            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

                    return PagedResponse.from(repository.findMetadataByStudentId(studentId, pageable));
                },
                result -> null
        );
    }

    public byte[] download(UUID studentId, UUID documentId, String email) {
        return audit.execute(
                AuditAction.MEDICAL_DOCUMENT_DOWNLOAD,
                studentId,
                documentId,
                email,
                () -> {
                    access.requireProfileReadAccess(studentId, email);

                    return repository.findByIdAndStudent_Id(documentId, studentId)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Laudo não encontrado."
                            ))
                            .getContent();
                },
                result -> documentId
        );
    }
}