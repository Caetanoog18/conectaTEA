package com.github.caetanoog18.conectatea.careteam.application;

import com.github.caetanoog18.conectatea.careteam.api.dto.CreateProfessionalLinkRequest;
import com.github.caetanoog18.conectatea.careteam.api.dto.EndProfessionalLinkRequest;
import com.github.caetanoog18.conectatea.careteam.api.dto.ProfessionalLinkResponse;
import com.github.caetanoog18.conectatea.careteam.application.exception.CareTeamConflictException;
import com.github.caetanoog18.conectatea.careteam.application.exception.CareTeamNotFoundException;
import com.github.caetanoog18.conectatea.careteam.application.exception.InvalidCareTeamLinkException;
import com.github.caetanoog18.conectatea.careteam.domain.StudentProfessionalLink;
import com.github.caetanoog18.conectatea.careteam.infrastructure.StudentProfessionalLinkRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CareTeamService {
    private static final Set<UserRole> PROFESSIONAL_ROLES = Set.of(
            UserRole.PEDAGOGICAL_COORDINATOR,
            UserRole.TEACHER,
            UserRole.AEE_TEACHER,
            UserRole.PSYCHOLOGIST,
            UserRole.PHYSICIAN
    );

    private final StudentProfessionalLinkRepository linkRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public CareTeamService(
            StudentProfessionalLinkRepository linkRepository,
            StudentRepository studentRepository,
            UserRepository userRepository
    ) {
        this.linkRepository = linkRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProfessionalLinkResponse create(UUID studentId, CreateProfessionalLinkRequest request, String authenticatedEmail) {
        User manager = requireActiveManager(authenticatedEmail);
        Student student = findStudent(studentId);
        User professional = findProfessional(request.professionalId());

        if (!student.isActive()) {
            throw new InvalidCareTeamLinkException("Student must be active");
        }

        if (!professional.isActive()) {
            throw new InvalidCareTeamLinkException("Professional must be active");
        }

        if (!PROFESSIONAL_ROLES.contains(professional.getRole())) {
            throw new InvalidCareTeamLinkException("User role is not eligible for the care team");
        }

        if (request.startedOn().isAfter(currentDate())) {
            throw new InvalidCareTeamLinkException("Start date cannot be in the future");
        }

        if (linkRepository.existsByStudent_IdAndProfessional_IdAndActiveTrue(studentId, professional.getId())) {
            throw new CareTeamConflictException("Professional already has an active link with this student");
        }

        StudentProfessionalLink link = new StudentProfessionalLink(
                student,
                professional,
                request.startedOn(),
                manager.getId()
        );

        try {
            return ProfessionalLinkResponse.from(linkRepository.saveAndFlush(link));
        } catch (DataIntegrityViolationException exception) {
            throw new CareTeamConflictException("Unable to create professional link because of a data conflict");
        }
    }

    public List<ProfessionalLinkResponse> findByStudent(UUID studentId, String authenticatedEmail) {
        requireActiveManager(authenticatedEmail);
        findStudent(studentId);

        return linkRepository
                .findAllByStudent_IdOrderByProfessional_FullNameAsc(
                        studentId
                )
                .stream()
                .map(ProfessionalLinkResponse::from)
                .toList();
    }

    public ProfessionalLinkResponse findById(UUID linkId, String authenticatedEmail) {
        requireActiveManager(authenticatedEmail);
        StudentProfessionalLink link = linkRepository
                .findById(linkId)
                .orElseThrow(() -> new CareTeamNotFoundException(
                        "Professional link not found: " + linkId
                ));

        return ProfessionalLinkResponse.from(link);
    }

    @Transactional
    public ProfessionalLinkResponse end(UUID linkId, EndProfessionalLinkRequest request, String authenticatedEmail) {
        User manager = requireActiveManager(authenticatedEmail);
        StudentProfessionalLink link = linkRepository
                .findByIdForUpdate(linkId)
                .orElseThrow(() -> new CareTeamNotFoundException("Professional link not found: " + linkId));

        if (!link.isActive()) {
            throw new CareTeamConflictException("Professional link is already inactive");
        }

        LocalDate endedOn = currentDate();

        if (endedOn.isBefore(link.getStartedOn())) {
            throw new InvalidCareTeamLinkException("End date cannot be before start date");
        }

        link.end(endedOn, manager.getId(), request.reason());

        return ProfessionalLinkResponse.from(linkRepository.saveAndFlush(link));
    }

    private Student findStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new CareTeamNotFoundException("Student not found: " + studentId));
    }

    private User findProfessional(UUID professionalId) {
        return userRepository.findById(professionalId)
                .orElseThrow(() -> new CareTeamNotFoundException("Professional user not found: " + professionalId));
    }

    private User requireActiveManager(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("An active manager account is required");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccessDeniedException("An active manager account is required"));

        boolean managerRole =
                user.getRole() == UserRole.ADMINISTRATOR || user.getRole() == UserRole.PEDAGOGICAL_COORDINATOR;

        if (!user.isActive() || !managerRole) {
            throw new AccessDeniedException("An active manager account is required");
        }

        return user;
    }

    private static LocalDate currentDate() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}