package com.github.caetanoog18.conectatea.guardian.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "guardians")
public class Guardian {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", unique = true)
    private UUID userId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 11)
    private String cpf;

    @Column(length = 254)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Guardian() {
    }

    public void update(
            String fullName,
            String cpf,
            String email,
            String phone,
            UUID userId
    ) {
        this.fullName = fullName.trim();
        this.cpf = normalizeDigits(cpf);
        this.email = normalizeEmail(email);
        this.phone = normalizeDigits(phone);
        this.userId = userId;
    }

    public Guardian(
            String fullName,
            String cpf,
            String email,
            String phone,
            UUID userId
    ) {
        update(fullName, cpf, email, phone, userId);
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
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

    private static String normalizeDigits(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.replaceAll("\\D", "");
    }

    private static String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}