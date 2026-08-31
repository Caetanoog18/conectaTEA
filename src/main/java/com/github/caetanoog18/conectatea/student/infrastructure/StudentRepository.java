package com.github.caetanoog18.conectatea.student.infrastructure;

import com.github.caetanoog18.conectatea.student.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository
        extends JpaRepository<Student, UUID> {

    Optional<Student> findByEnrollmentNumberIgnoreCase(
            String enrollmentNumber
    );

    boolean existsByEnrollmentNumberIgnoreCase(
            String enrollmentNumber
    );

    boolean existsByEnrollmentNumberIgnoreCaseAndIdNot(
            String enrollmentNumber,
            UUID id
    );
}