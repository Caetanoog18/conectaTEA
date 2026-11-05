package com.github.caetanoog18.conectatea.careteam.application;

import com.github.caetanoog18.conectatea.careteam.api.dto.AccessibleStudentResponse;
import com.github.caetanoog18.conectatea.careteam.api.dto.LegalGuardianContactResponse;
import com.github.caetanoog18.conectatea.careteam.infrastructure.StudentProfessionalLinkRepository;
import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.consent.domain.ConsentStatus;
import com.github.caetanoog18.conectatea.consent.infrastructure.ConsentTermRepository;
import com.github.caetanoog18.conectatea.guardian.infrastructure.StudentGuardianRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentAccessService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentProfessionalLinkRepository linkRepository;
    private final ConsentTermRepository consentRepository;
    private final StudentGuardianRepository guardianLinkRepository;

    public StudentAccessService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            StudentProfessionalLinkRepository linkRepository,
            ConsentTermRepository consentRepository,
            StudentGuardianRepository guardianLinkRepository
    ) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.linkRepository = linkRepository;
        this.consentRepository = consentRepository;
        this.guardianLinkRepository = guardianLinkRepository;
    }

    public PagedResponse<AccessibleStudentResponse> findAccessibleStudents(int page, int size, String authenticatedEmail) {
        User professional = requireProfessional(authenticatedEmail);
        ConsentPurpose purpose = requiredPurpose(professional.getRole());

        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("id"))
        );

        var students = studentRepository.findAccessibleToProfessional(
                professional.getId(),
                ConsentStatus.ACTIVE,
                now,
                today,
                purpose,
                ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM,
                pageable
        );

        return PagedResponse.from(students.map(AccessibleStudentResponse::from));
    }

    public Student requireProfileReadAccess(UUID studentId, String authenticatedEmail) {
        if (studentId == null) {
            throw denied();
        }

        User professional = requireProfessional(authenticatedEmail);
        ConsentPurpose purpose = requiredPurpose(professional.getRole());

        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        return requireStudentAccess(
                studentId,
                professional,
                purpose,
                now,
                today
        );
    }

    public List<LegalGuardianContactResponse> findLegalGuardianContacts(UUID studentId, String authenticatedEmail) {
        if (studentId == null) {
            throw denied();
        }

        User professional = requireProfessional(authenticatedEmail);
        ConsentPurpose purpose = requiredPurpose(professional.getRole());

        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        requireStudentAccess(
                studentId,
                professional,
                purpose,
                now,
                today
        );

        return guardianLinkRepository
                .findAuthorizedLegalGuardianContacts(
                        studentId,
                        ConsentStatus.ACTIVE,
                        now,
                        today,
                        purpose,
                        ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
                )
                .stream()
                .map(LegalGuardianContactResponse::from)
                .toList();
    }

    private Student requireStudentAccess(
            UUID studentId,
            User professional,
            ConsentPurpose purpose,
            Instant now,
            LocalDate today
    ) {
        Student student = studentRepository.findById(studentId)
                .filter(Student::isActive)
                .orElseThrow(StudentAccessService::denied);

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
                .countValidConsentsForPurposes(
                        studentId,
                        ConsentStatus.ACTIVE,
                        now,
                        today,
                        purpose,
                        ConsentPurpose.INFORMATION_SHARING_WITH_CARE_TEAM
                );

        if (authorizingConsents == 0) {
            throw denied();
        }

        return student;
    }

    private User requireProfessional(String email) {
        if (email == null || email.isBlank()) {
            throw denied();
        }

        User professional = userRepository.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .orElseThrow(StudentAccessService::denied);

        requiredPurpose(professional.getRole());

        return professional;
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