package com.github.caetanoog18.conectatea.guardian.domain;

import com.github.caetanoog18.conectatea.student.domain.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_guardians")
public class StudentGuardian {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GuardianRelationship relationship;

    @Column(name = "legal_guardian", nullable = false)
    private boolean legalGuardian;

    @Column(name = "primary_contact", nullable = false)
    private boolean primaryContact;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentGuardian() {
    }

    public void update(GuardianRelationship relationship, boolean legalGuardian, boolean primaryContact) {
        this.relationship = relationship;
        this.legalGuardian = legalGuardian;
        this.primaryContact = primaryContact;
    }

    public StudentGuardian(
            Student student,
            Guardian guardian,
            GuardianRelationship relationship,
            boolean legalGuardian,
            boolean primaryContact
    ) {
        this.student = student;
        this.guardian = guardian;
        update(relationship, legalGuardian, primaryContact);
    }

    public UUID getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public Guardian getGuardian() {
        return guardian;
    }

    public GuardianRelationship getRelationship() {
        return relationship;
    }

    public boolean isLegalGuardian() {
        return legalGuardian;
    }

    public boolean isPrimaryContact() {
        return primaryContact;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}