package com.github.caetanoog18.conectatea.careteam.infrastructure;

import com.github.caetanoog18.conectatea.careteam.domain.StudentProfessionalLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentProfessionalLinkRepository extends JpaRepository<StudentProfessionalLink, UUID> {
    boolean existsByStudent_IdAndProfessional_IdAndActiveTrue(UUID studentId, UUID professionalId);

    List<StudentProfessionalLink>
    findAllByStudent_IdOrderByProfessional_FullNameAsc(UUID studentId);

    List<StudentProfessionalLink>
    findAllByProfessional_IdAndActiveTrueOrderByStudent_FullNameAsc(UUID professionalId);
}