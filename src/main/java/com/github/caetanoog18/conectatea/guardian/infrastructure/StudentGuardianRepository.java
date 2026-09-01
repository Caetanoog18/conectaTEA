package com.github.caetanoog18.conectatea.guardian.infrastructure;

import com.github.caetanoog18.conectatea.guardian.domain.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
