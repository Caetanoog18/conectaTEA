package com.github.caetanoog18.conectatea.guardian.infrastructure;

import com.github.caetanoog18.conectatea.guardian.domain.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, UUID> {
}
