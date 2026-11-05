package com.github.caetanoog18.conectatea.medicaldocument.infrastructure;

import com.github.caetanoog18.conectatea.medicaldocument.api.dto.MedicalDocumentResponse;
import com.github.caetanoog18.conectatea.medicaldocument.domain.MedicalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, UUID> {
    @Query("""
            select new com.github.caetanoog18.conectatea.medicaldocument.api.dto.MedicalDocumentResponse(
                d.id,
                d.student.id,
                d.title,
                d.issuedOn,
                d.uploadedBy.id,
                d.uploadedBy.fullName,
                d.sizeBytes,
                d.createdAt
            )
            from MedicalDocument d
            where d.student.id = :studentId
            """)
    Page<MedicalDocumentResponse> findMetadataByStudentId(@Param("studentId") UUID studentId, Pageable pageable);

    Optional<MedicalDocument> findByIdAndStudent_Id(UUID id, UUID studentId);
}