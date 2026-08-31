package com.github.caetanoog18.conectatea.institution.infrastructure;

import com.github.caetanoog18.conectatea.institution.domain.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InstitutionRepository
        extends JpaRepository<Institution, UUID> {
}