package com.github.caetanoog18.conectatea.guardian.application;

import com.github.caetanoog18.conectatea.guardian.api.dto.CreateStudentGuardianLinkRequest;
import com.github.caetanoog18.conectatea.guardian.api.dto.StudentGuardianResponse;
import com.github.caetanoog18.conectatea.guardian.api.dto.UpdateStudentGuardianLinkRequest;
import com.github.caetanoog18.conectatea.guardian.application.exception.GuardianNotFoundException;
import com.github.caetanoog18.conectatea.guardian.application.exception.InactiveStudentGuardianException;
import com.github.caetanoog18.conectatea.guardian.application.exception.PrimaryContactAlreadyExistsException;
import com.github.caetanoog18.conectatea.guardian.application.exception.StudentGuardianLinkAlreadyExistsException;
import com.github.caetanoog18.conectatea.guardian.application.exception.StudentGuardianLinkConflictException;
import com.github.caetanoog18.conectatea.guardian.application.exception.StudentGuardianLinkNotFoundException;
import com.github.caetanoog18.conectatea.guardian.domain.Guardian;
import com.github.caetanoog18.conectatea.guardian.domain.StudentGuardian;
import com.github.caetanoog18.conectatea.guardian.infrastructure.GuardianRepository;
import com.github.caetanoog18.conectatea.guardian.infrastructure.StudentGuardianRepository;
import com.github.caetanoog18.conectatea.student.application.exception.StudentNotFoundException;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentGuardianService {
    private final StudentGuardianRepository linkRepository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;

    public StudentGuardianService(StudentGuardianRepository linkRepository,
            StudentRepository studentRepository,
            GuardianRepository guardianRepository
    ) {
        this.linkRepository = linkRepository;
        this.studentRepository = studentRepository;
        this.guardianRepository = guardianRepository;
    }

    @Transactional
    public StudentGuardianResponse create(UUID studentId, CreateStudentGuardianLinkRequest request) {
        Student student = findStudent(studentId);
        Guardian guardian = findGuardian(request.guardianId());

        if (!student.isActive() || !guardian.isActive()) {
            throw new InactiveStudentGuardianException();
        }

        if (linkRepository.existsByStudent_IdAndGuardian_Id(
                studentId,
                request.guardianId()
        )) {
            throw new StudentGuardianLinkAlreadyExistsException();
        }

        validatePrimaryContact(
                studentId,
                request.guardianId(),
                request.primaryContact(),
                false
        );
        StudentGuardian link = new StudentGuardian(
                student,
                guardian,
                request.relationship(),
                request.legalGuardian(),
                request.primaryContact()
        );
        return StudentGuardianResponse.from(save(link));
    }

    public List<StudentGuardianResponse> findByStudent(UUID studentId) {
        findStudent(studentId);
        return linkRepository
                .findAllByStudent_IdOrderByGuardian_FullNameAsc(
                        studentId
                )
                .stream()
                .map(StudentGuardianResponse::from)
                .toList();
    }

    public List<StudentGuardianResponse> findByGuardian(
            UUID guardianId
    ) {
        findGuardian(guardianId);
        return linkRepository
                .findAllByGuardian_IdOrderByStudent_FullNameAsc(
                        guardianId
                )
                .stream()
                .map(StudentGuardianResponse::from)
                .toList();
    }

    @Transactional
    public StudentGuardianResponse update(
            UUID studentId,
            UUID guardianId,
            UpdateStudentGuardianLinkRequest request
    ) {
        StudentGuardian link = findLink(studentId, guardianId);

        validatePrimaryContact(
                studentId,
                guardianId,
                request.primaryContact(),
                true
        );

        link.update(
                request.relationship(),
                request.legalGuardian(),
                request.primaryContact()
        );

        return StudentGuardianResponse.from(save(link));
    }

    @Transactional
    public void delete(UUID studentId, UUID guardianId) {
        StudentGuardian link = findLink(studentId, guardianId);

        linkRepository.delete(link);
        linkRepository.flush();
    }

    private Student findStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(
                        () -> new StudentNotFoundException(studentId)
                );
    }

    private Guardian findGuardian(UUID guardianId) {
        return guardianRepository.findById(guardianId)
                .orElseThrow(
                        () -> new GuardianNotFoundException(guardianId)
                );
    }

    private StudentGuardian findLink(
            UUID studentId,
            UUID guardianId
    ) {
        return linkRepository
                .findByStudent_IdAndGuardian_Id(
                        studentId,
                        guardianId
                )
                .orElseThrow(
                        StudentGuardianLinkNotFoundException::new
                );
    }

    private void validatePrimaryContact(
            UUID studentId,
            UUID guardianId,
            boolean primaryContact,
            boolean updating
    ) {
        if (!primaryContact) {
            return;
        }

        boolean exists = updating
                ? linkRepository
                .existsByStudent_IdAndPrimaryContactTrueAndGuardian_IdNot(
                        studentId,
                        guardianId
                )
                : linkRepository
                .existsByStudent_IdAndPrimaryContactTrue(
                        studentId
                );

        if (exists) {
            throw new PrimaryContactAlreadyExistsException();
        }
    }

    private StudentGuardian save(StudentGuardian link) {
        try {
            return linkRepository.saveAndFlush(link);
        } catch (DataIntegrityViolationException exception) {
            throw new StudentGuardianLinkConflictException();
        }
    }
}