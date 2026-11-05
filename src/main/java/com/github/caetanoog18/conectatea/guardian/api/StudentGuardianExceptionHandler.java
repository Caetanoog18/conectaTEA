package com.github.caetanoog18.conectatea.guardian.api;

import com.github.caetanoog18.conectatea.guardian.application.exception.InactiveStudentGuardianException;
import com.github.caetanoog18.conectatea.guardian.application.exception.PrimaryContactAlreadyExistsException;
import com.github.caetanoog18.conectatea.guardian.application.exception.StudentGuardianLinkAlreadyExistsException;
import com.github.caetanoog18.conectatea.guardian.application.exception.StudentGuardianLinkConflictException;
import com.github.caetanoog18.conectatea.guardian.application.exception.StudentGuardianLinkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class StudentGuardianExceptionHandler {
    @ExceptionHandler(StudentGuardianLinkNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(StudentGuardianLinkNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "Student guardian link not found", exception.getMessage());
    }

    @ExceptionHandler({
            StudentGuardianLinkAlreadyExistsException.class,
            PrimaryContactAlreadyExistsException.class,
            StudentGuardianLinkConflictException.class
    })
    public ResponseEntity<ProblemDetail> handleConflict(RuntimeException exception) {
        return build(HttpStatus.CONFLICT, "Student guardian link conflict", exception.getMessage());
    }

    @ExceptionHandler(InactiveStudentGuardianException.class)
    public ResponseEntity<ProblemDetail> handleInactive(InactiveStudentGuardianException exception) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Inactive student or guardian",
                exception.getMessage());
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}