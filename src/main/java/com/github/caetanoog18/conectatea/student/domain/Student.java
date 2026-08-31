package com.github.caetanoog18.conectatea.student.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "preferred_name", length = 120)
    private String preferredName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "enrollment_number", nullable = false, length = 50)
    private String enrollmentNumber;

    @Column(name = "school_year", nullable = false)
    private int schoolYear;

    @Column(name = "grade_level", nullable = false, length = 50)
    private String gradeLevel;

    @Column(name = "class_name", length = 30)
    private String className;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Student() {
    }

    public void update(
            String fullName,
            String preferredName,
            LocalDate birthDate,
            String enrollmentNumber,
            int schoolYear,
            String gradeLevel,
            String className
    ) {
        this.fullName = fullName.trim();
        this.preferredName = normalizeOptionalText(preferredName);
        this.birthDate = birthDate;
        this.enrollmentNumber = enrollmentNumber
                .trim()
                .toUpperCase(Locale.ROOT);
        this.schoolYear = schoolYear;
        this.gradeLevel = gradeLevel.trim();
        this.className = normalizeOptionalText(className);
    }

    public Student(
            String fullName,
            String preferredName,
            LocalDate birthDate,
            String enrollmentNumber,
            int schoolYear,
            String gradeLevel,
            String className
    ) {
        update(
                fullName,
                preferredName,
                birthDate,
                enrollmentNumber,
                schoolYear,
                gradeLevel,
                className
        );

        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getEnrollmentNumber() {
        return enrollmentNumber;
    }

    public int getSchoolYear() {
        return schoolYear;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public String getClassName() {
        return className;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}