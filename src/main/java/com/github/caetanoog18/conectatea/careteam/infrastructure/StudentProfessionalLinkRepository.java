package com.github.caetanoog18.conectatea.careteam.infrastructure;

import com.github.caetanoog18.conectatea.careteam.domain.StudentProfessionalLink;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

public interface StudentProfessionalLinkRepository extends JpaRepository<StudentProfessionalLink, UUID> {
    boolean existsByStudent_IdAndProfessional_IdAndActiveTrue(UUID studentId, UUID professionalId);
    boolean existsByStudent_IdAndProfessional_IdAndActiveTrueAndStartedOnLessThanEqual(
            UUID studentId,
            UUID professionalId,
            LocalDate referenceDate);

    List<StudentProfessionalLink>
    findAllByStudent_IdOrderByProfessional_FullNameAsc(UUID studentId);

    List<StudentProfessionalLink>
    findAllByProfessional_IdAndActiveTrueOrderByStudent_FullNameAsc(UUID professionalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select l
            from StudentProfessionalLink l
            where l.id = :linkId
            """
    )
    Optional<StudentProfessionalLink> findByIdForUpdate(@Param("linkId") UUID linkId);
}