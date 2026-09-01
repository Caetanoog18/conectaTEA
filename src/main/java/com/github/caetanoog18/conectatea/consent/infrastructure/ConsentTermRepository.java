package com.github.caetanoog18.conectatea.consent.infrastructure;

import com.github.caetanoog18.conectatea.consent.domain.ConsentStatus;
import com.github.caetanoog18.conectatea.consent.domain.ConsentTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentTermRepository extends JpaRepository<ConsentTerm, UUID> {
    Optional<ConsentTerm> findByStudentGuardian_IdAndStatus(
            UUID studentGuardianId,
            ConsentStatus status
    );

    List<ConsentTerm> findAllByStudentGuardian_IdOrderByGrantedAtDesc(UUID studentGuardianId);

    boolean existsByStudentGuardian_IdAndStatus(UUID studentGuardianId, ConsentStatus status);
}