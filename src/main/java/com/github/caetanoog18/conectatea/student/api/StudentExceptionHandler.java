package com.github.caetanoog18.conectatea.student.api;

import com.github.caetanoog18.conectatea.student.application.exception.EnrollmentNumberAlreadyInUseException;
import com.github.caetanoog18.conectatea.student.application.exception.StudentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class StudentExceptionHandler {
    @ExceptionHandler(EnrollmentNumberAlreadyInUseException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEnrollment(
            EnrollmentNumberAlreadyInUseException exception
    ) {
        return buildProblem(
                HttpStatus.CONFLICT,
                "Enrollment number already in use",
                exception.getMessage()
        );
    }

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            StudentNotFoundException exception
    ) {
        return buildProblem(
                HttpStatus.NOT_FOUND,
                "Student not found",
                exception.getMessage()
        );
    }

    private ResponseEntity<ProblemDetail> buildProblem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);

        return ResponseEntity
                .status(status)
                .body(problem);
    }
}