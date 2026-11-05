package com.github.caetanoog18.conectatea.observation.domain;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.identity.domain.User;
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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "observations")
public class Observation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, updatable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false, updatable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private ConsentPurpose purpose;

    @Column(nullable = false, length = 120, updatable = false)
    private String title;

    @Column(nullable = false, length = 5000, updatable = false)
    private String content;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Observation() {
    }

    public Observation(
            Student student,
            User author,
            ConsentPurpose purpose,
            String title,
            String content,
            Instant occurredAt
    ) {
        this.student = Objects.requireNonNull(student, "Student is required");
        this.author = Objects.requireNonNull(author, "Author is required");

        if (purpose != ConsentPurpose.EDUCATIONAL_SUPPORT && purpose != ConsentPurpose.MULTIPROFESSIONAL_MONITORING) {
            throw new IllegalArgumentException("Invalid observation purpose");
        }

        this.purpose = purpose;
        this.title = requiredText(title, 120, "Title");
        this.content = requiredText(content, 5000, "Content");

        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurrence date is required");

        if (occurredAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Occurrence date cannot be in the future");
        }
    }

    private static String requiredText(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }

        String normalized = value.strip();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " exceeds maximum length");
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public User getAuthor() {
        return author;
    }

    public ConsentPurpose getPurpose() {
        return purpose;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}