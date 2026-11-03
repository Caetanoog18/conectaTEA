package com.github.caetanoog18.conectatea.guardian.infrastructure;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.consent.domain.ConsentStatus;
import com.github.caetanoog18.conectatea.guardian.domain.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, UUID> {
    Optional<StudentGuardian> findByStudent_IdAndGuardian_Id(UUID studentId, UUID guardianId);

    boolean existsByStudent_IdAndGuardian_Id(UUID studentId, UUID guardianId);

    List<StudentGuardian>
    findAllByStudent_IdOrderByGuardian_FullNameAsc(UUID studentId);

    List<StudentGuardian>
    findAllByGuardian_IdOrderByStudent_FullNameAsc(UUID guardianId);

    boolean existsByStudent_IdAndPrimaryContactTrue(UUID studentId);
    boolean existsByStudent_IdAndPrimaryContactTrueAndGuardian_IdNot(UUID studentId, UUID guardianId);

    @Query("""
            select sg
            from StudentGuardian sg
            join fetch sg.guardian g
            where sg.student.id = :studentId
              and sg.legalGuardian = true
              and g.active = true
              and exists (
                  select c.id
                  from ConsentTerm c
                  where c.studentGuardian = sg
                    and c.status = :status
                    and c.grantedAt <= :referenceInstant
                    and (
                        c.validUntil is null
                        or c.validUntil >= :referenceDate
                    )
                    and :requiredPurpose member of c.purposes
                    and :sharingPurpose member of c.purposes
              )
            order by sg.primaryContact desc, g.fullName asc, sg.id asc
            """)
    List<StudentGuardian> findAuthorizedLegalGuardianContacts(
            @Param("studentId") UUID studentId,
            @Param("status") ConsentStatus status,
            @Param("referenceInstant") Instant referenceInstant,
            @Param("referenceDate") LocalDate referenceDate,
            @Param("requiredPurpose") ConsentPurpose requiredPurpose,
            @Param("sharingPurpose") ConsentPurpose sharingPurpose
    );
}