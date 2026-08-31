package com.github.caetanoog18.conectatea.institution.domain;

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
@Table(name = "institutions")
public class Institution {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "tax_id", length = 14)
    private String taxId;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 160)
    private String street;

    @Column(name = "address_number", length = 20)
    private String addressNumber;

    @Column(length = 100)
    private String complement;

    @Column(length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 2)
    private String state;

    @Column(name = "postal_code", length = 8)
    private String postalCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Institution() {}

    public Institution(
            String name,
            String taxId,
            String email,
            String phone,
            String street,
            String addressNumber,
            String complement,
            String district,
            String city,
            String state,
            String postalCode
    ) {
        this.name = name.trim();
        this.taxId = normalizeDigits(taxId);
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.phone = normalizeDigits(phone);
        this.street = normalizeOptionalText(street);
        this.addressNumber = normalizeOptionalText(addressNumber);
        this.complement = normalizeOptionalText(complement);
        this.district = normalizeOptionalText(district);
        this.city = city.trim();
        this.state = state.trim().toUpperCase(Locale.ROOT);
        this.postalCode = normalizeDigits(postalCode);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getStreet() {
        return street;
    }

    public String getAddressNumber() {
        return addressNumber;
    }

    public String getComplement() {
        return complement;
    }

    public String getDistrict() {
        return district;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalizeDigits(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}