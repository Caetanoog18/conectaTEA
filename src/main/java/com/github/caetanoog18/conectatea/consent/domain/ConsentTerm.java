package com.github.caetanoog18.conectatea.consent.domain;

import com.github.caetanoog18.conectatea.guardian.domain.StudentGuardian;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "consent_terms")
public class ConsentTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_guardian_id", nullable = false)
    private StudentGuardian studentGuardian;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsentStatus status;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "consent_term_purposes", joinColumns = @JoinColumn(name = "consent_term_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 60)
    private Set<ConsentPurpose> purposes = new HashSet<>();

    @Column(name = "terms_version", nullable = false, length = 20)
    private String termsVersion;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "recorded_by_user_id", nullable = false)
    private UUID recordedByUserId;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by_user_id")
    private UUID revokedByUserId;

    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConsentTerm() {
    }

    public ConsentTerm(
            StudentGuardian studentGuardian,
            Set<ConsentPurpose> purposes,
            String termsVersion,
            Instant grantedAt,
            LocalDate validUntil,
            UUID recordedByUserId
    ) {
        if (purposes == null || purposes.isEmpty()) {
            throw new IllegalArgumentException("At least one consent purpose is required");
        }

        this.studentGuardian = studentGuardian;
        this.purposes = new HashSet<>(purposes);
        this.termsVersion = termsVersion;
        this.grantedAt = grantedAt;
        this.validUntil = validUntil;
        this.recordedByUserId = recordedByUserId;
        this.status = ConsentStatus.ACTIVE;
    }

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();

        if (grantedAt == null) {
            grantedAt = now;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    public void revoke(Instant revokedAt, UUID revokedByUserId, String revocationReason) {
        if (status == ConsentStatus.REVOKED) {
            throw new IllegalStateException("Consent term is already revoked");
        }

        this.status = ConsentStatus.REVOKED;
        this.revokedAt = revokedAt;
        this.revokedByUserId = revokedByUserId;
        this.revocationReason = revocationReason;
    }

    public boolean isExpired(LocalDate referenceDate) {
        return validUntil != null && validUntil.isBefore(referenceDate);
    }

    public UUID getId() {
        return id;
    }

    public StudentGuardian getStudentGuardian() {
        return studentGuardian;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public Set<ConsentPurpose> getPurposes() {
        return Set.copyOf(purposes);
    }

    public String getTermsVersion() {
        return termsVersion;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public UUID getRecordedByUserId() {
        return recordedByUserId;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getRevokedByUserId() {
        return revokedByUserId;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}