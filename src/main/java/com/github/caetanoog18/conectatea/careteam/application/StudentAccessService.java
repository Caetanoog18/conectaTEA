package com.github.caetanoog18.conectatea.careteam.application;

import com.github.caetanoog18.conectatea.careteam.infrastructure.StudentProfessionalLinkRepository;
import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.consent.domain.ConsentStatus;
import com.github.caetanoog18.conectatea.consent.infrastructure.ConsentTermRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentAccessService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentProfessionalLinkRepository linkRepository;
    private final ConsentTermRepository consentRepository;

    public StudentAccessService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            StudentProfessionalLinkRepository linkRepository,
            ConsentTermRepository consentRepository
    ) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.linkRepository = linkRepository;
        this.consentRepository = consentRepository;
    }

    public Student requireProfileReadAccess(UUID studentId, String authenticatedEmail) {
        if (studentId == null || authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw denied();
        }

        User professional = userRepository
                .findByEmailIgnoreCase(authenticatedEmail)
                .filter(User::isActive)
                .orElseThrow(StudentAccessService::denied);

        ConsentPurpose requiredPurpose = requiredPurpose(
                professional.getRole()
        );

        Student student = studentRepository
                .findById(studentId)
                .filter(Student::isActive)
                .orElseThrow(StudentAccessService::denied);

        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        boolean hasActiveLink = linkRepository
                .existsByStudent_IdAndProfessional_IdAndActiveTrueAndStartedOnLessThanEqual(
                        studentId,
                        professional.getId(),
                        today
                );

        if (!hasActiveLink) {
            throw denied();
        }

        long authorizingConsents = consentRepository
                .countAuthorizingProfileRead(
                        studentId,
                        ConsentStatus.ACTIVE,
                        now,
                        today,
                        requiredPurpose,
                        ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
                );

        if (authorizingConsents == 0) {
            throw denied();
        }

        return student;
    }

    private static ConsentPurpose requiredPurpose(UserRole role) {
        return switch (role) {
            case TEACHER, AEE_TEACHER -> ConsentPurpose.EDUCATIONAL_SUPPORT;
            case PEDAGOGICAL_COORDINATOR, PSYCHOLOGIST, PHYSICIAN -> ConsentPurpose.MULTIPROFESSIONAL_MONITORING;
            default -> throw denied();
        };
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("Access to this student is not authorized");
    }
}