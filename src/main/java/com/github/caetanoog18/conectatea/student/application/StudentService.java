package com.github.caetanoog18.conectatea.student.application;

import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import com.github.caetanoog18.conectatea.student.api.dto.StudentRequest;
import com.github.caetanoog18.conectatea.student.api.dto.StudentResponse;
import com.github.caetanoog18.conectatea.student.api.dto.UpdateStudentStatusRequest;
import com.github.caetanoog18.conectatea.student.application.exception.EnrollmentNumberAlreadyInUseException;
import com.github.caetanoog18.conectatea.student.application.exception.StudentNotFoundException;
import com.github.caetanoog18.conectatea.student.domain.Student;
import com.github.caetanoog18.conectatea.student.infrastructure.StudentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        if (studentRepository.existsByEnrollmentNumberIgnoreCase(
                request.enrollmentNumber()
        )) {
            throw new EnrollmentNumberAlreadyInUseException();
        }

        Student student = new Student(
                request.fullName(),
                request.preferredName(),
                request.birthDate(),
                request.enrollmentNumber(),
                request.schoolYear(),
                request.gradeLevel(),
                request.className()
        );

        return StudentResponse.from(save(student));
    }

    public PagedResponse<StudentResponse> findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "fullName"));

        Page<StudentResponse> students = studentRepository
                .findAll(pageable)
                .map(StudentResponse::from);

        return PagedResponse.from(students);
    }

    public StudentResponse findById(UUID studentId) {
        return StudentResponse.from(findStudent(studentId));
    }

    @Transactional
    public StudentResponse update(UUID studentId, StudentRequest request) {
        Student student = findStudent(studentId);

        if (studentRepository
                .existsByEnrollmentNumberIgnoreCaseAndIdNot(
                        request.enrollmentNumber(),
                        studentId
                )) {
            throw new EnrollmentNumberAlreadyInUseException();
        }

        student.update(
                request.fullName(),
                request.preferredName(),
                request.birthDate(),
                request.enrollmentNumber(),
                request.schoolYear(),
                request.gradeLevel(),
                request.className()
        );

        return StudentResponse.from(save(student));
    }

    @Transactional
    public StudentResponse updateStatus(UUID studentId, UpdateStudentStatusRequest request) {
        Student student = findStudent(studentId);

        if (request.active()) {
            student.activate();
        } else {
            student.deactivate();
        }

        return StudentResponse.from(studentRepository.saveAndFlush(student));
    }

    private Student findStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
    }

    private Student save(Student student) {
        try {
            return studentRepository.saveAndFlush(student);
        } catch (DataIntegrityViolationException exception) {
            throw new EnrollmentNumberAlreadyInUseException();
        }
    }
}