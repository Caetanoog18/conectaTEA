package com.github.caetanoog18.conectatea.student.infrastructure;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.consent.domain.ConsentStatus;
import com.github.caetanoog18.conectatea.student.domain.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByEnrollmentNumberIgnoreCase(String enrollmentNumber);

    boolean existsByEnrollmentNumberIgnoreCase(String enrollmentNumber);
    boolean existsByEnrollmentNumberIgnoreCaseAndIdNot(String enrollmentNumber, UUID id);
    boolean existsByCpf(String cpf);
    boolean existsByCpfAndIdNot(String cpf, UUID id);

    @Query("""
            select s
            from Student s
            where s.active = true
              and exists (
                  select l.id
                  from StudentProfessionalLink l
                  where l.student = s
                    and l.professional.id = :professionalId
                    and l.active = true
                    and l.startedOn <= :referenceDate
              )
              and exists (
                  select c.id
                  from ConsentTerm c
                  join c.studentGuardian sg
                  join sg.guardian g
                  where sg.student = s
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
              )
            """)
    Page<Student> findAccessibleToProfessional(
            @Param("professionalId") UUID professionalId,
            @Param("status") ConsentStatus status,
            @Param("referenceInstant") Instant referenceInstant,
            @Param("referenceDate") LocalDate referenceDate,
            @Param("requiredPurpose") ConsentPurpose requiredPurpose,
            @Param("sharingPurpose") ConsentPurpose sharingPurpose,
            Pageable pageable
    );
}