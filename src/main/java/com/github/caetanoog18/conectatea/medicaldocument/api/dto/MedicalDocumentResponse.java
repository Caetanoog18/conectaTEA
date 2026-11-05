package com.github.caetanoog18.conectatea.medicaldocument.api.dto;

import com.github.caetanoog18.conectatea.medicaldocument.domain.MedicalDocument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MedicalDocumentResponse(
        UUID id,
        UUID studentId,
        String title,
        LocalDate issuedOn,
        UUID uploadedByUserId,
        String uploadedByName,
        long sizeBytes,
        Instant createdAt
) {
    public static MedicalDocumentResponse from(MedicalDocument document) {
        return new MedicalDocumentResponse(
                document.getId(),
                document.getStudent().getId(),
                document.getTitle(),
                document.getIssuedOn(),
                document.getUploadedBy().getId(),
                document.getUploadedBy().getFullName(),
                document.getSizeBytes(),
                document.getCreatedAt()
        );
    }
}