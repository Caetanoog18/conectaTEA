package com.github.caetanoog18.conectatea.institution.api.dto;

import com.github.caetanoog18.conectatea.institution.domain.Institution;

import java.time.Instant;
import java.util.UUID;

public record InstitutionResponse(
        UUID id,
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
        String postalCode,
        Instant createdAt,
        Instant updatedAt
) {
    public static InstitutionResponse from(Institution institution) {
        return new InstitutionResponse(
                institution.getId(),
                institution.getName(),
                institution.getTaxId(),
                institution.getEmail(),
                institution.getPhone(),
                institution.getStreet(),
                institution.getAddressNumber(),
                institution.getComplement(),
                institution.getDistrict(),
                institution.getCity(),
                institution.getState(),
                institution.getPostalCode(),
                institution.getCreatedAt(),
                institution.getUpdatedAt()
        );
    }
}