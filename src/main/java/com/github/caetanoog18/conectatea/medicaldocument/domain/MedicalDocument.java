package com.github.caetanoog18.conectatea.medicaldocument.domain;

import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.student.domain.Student;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medical_documents")
public class MedicalDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "student_id",
            nullable = false,
            updatable = false
    )
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "uploaded_by_user_id",
            nullable = false,
            updatable = false
    )
    private User uploadedBy;

    @Column(nullable = false, length = 120, updatable = false)
    private String title;

    @Column(name = "issued_on", nullable = false, updatable = false)
    private LocalDate issuedOn;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(nullable = false, columnDefinition = "bytea", updatable = false)
    private byte[] content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MedicalDocument() {
    }

    public MedicalDocument(
            Student student,
            User uploadedBy,
            String title,
            LocalDate issuedOn,
            byte[] content
    ) {
        this.student = student;
        this.uploadedBy = uploadedBy;
        this.title = title;
        this.issuedOn = issuedOn;
        this.content = content.clone();
        this.sizeBytes = content.length;
    }

    public UUID getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public byte[] getContent() {
        return content.clone();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}