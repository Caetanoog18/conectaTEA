package com.github.caetanoog18.conectatea.consent.infrastructure;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.consent.domain.ConsentStatus;
import com.github.caetanoog18.conectatea.consent.domain.ConsentTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentTermRepository extends JpaRepository<ConsentTerm, UUID> {
    Optional<ConsentTerm> findByStudentGuardian_IdAndStatus(UUID studentGuardianId, ConsentStatus status);
    List<ConsentTerm> findAllByStudentGuardian_IdOrderByGrantedAtDesc(UUID studentGuardianId);
    boolean existsByStudentGuardian_IdAndStatus(UUID studentGuardianId, ConsentStatus status);

    @Query("""
            select count(c)
            from ConsentTerm c
            join c.studentGuardian sg
            join sg.guardian g
            where sg.student.id = :studentId
              and sg.legalGuardian = true
              and g.active = true
              and c.status = :status
              and c.grantedAt <= :referenceInstant
              and (
                  c.validUntil is null
                  or c.validUntil >= :referenceDate
              )
              and :requiredPurpose member of c.purposes
              and :sharingPurpose member of c.purposes
            """)

    long countValidConsentsForPurposes(
            @Param("studentId") UUID studentId,
            @Param("status") ConsentStatus status,
            @Param("referenceInstant") Instant referenceInstant,
            @Param("referenceDate") LocalDate referenceDate,
            @Param("requiredPurpose") ConsentPurpose requiredPurpose,
            @Param("sharingPurpose") ConsentPurpose sharingPurpose
    );
}