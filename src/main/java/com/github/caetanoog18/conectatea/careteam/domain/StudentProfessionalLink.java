package com.github.caetanoog18.conectatea.careteam.domain;

import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.student.domain.Student;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_professional_links")
public class StudentProfessionalLink {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_user_id", nullable = false)
    private User professional;

    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;

    @Column(name = "ended_on")
    private LocalDate endedOn;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "ended_by_user_id")
    private UUID endedByUserId;

    @Column(name = "end_reason", length = 500)
    private String endReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentProfessionalLink() {
    }

    public StudentProfessionalLink(Student student, User professional, LocalDate startedOn, UUID createdByUserId) {
        this.student = student;
        this.professional = professional;
        this.startedOn = startedOn;
        this.createdByUserId = createdByUserId;
        this.active = true;
    }

    public void end(LocalDate endedOn, UUID endedByUserId, String endReason) {
        if (!active) {
            throw new IllegalStateException("Professional link is already inactive");
        }

        if (endedOn.isBefore(startedOn)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        if (endReason == null || endReason.isBlank()) {
            throw new IllegalArgumentException("End reason is required");
        }

        this.endedOn = endedOn;
        this.endedByUserId = endedByUserId;
        this.endReason = endReason.trim();
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public User getProfessional() {
        return professional;
    }

    public LocalDate getStartedOn() {
        return startedOn;
    }

    public LocalDate getEndedOn() {
        return endedOn;
    }

    public boolean isActive() {
        return active;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public UUID getEndedByUserId() {
        return endedByUserId;
    }

    public String getEndReason() {
        return endReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}