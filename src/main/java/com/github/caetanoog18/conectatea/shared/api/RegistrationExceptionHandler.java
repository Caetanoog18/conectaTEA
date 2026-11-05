package com.github.caetanoog18.conectatea.shared.api;

import com.github.caetanoog18.conectatea.shared.domain.CpfAlreadyInUseException;
import com.github.caetanoog18.conectatea.shared.domain.InvalidCpfException;
import com.github.caetanoog18.conectatea.shared.domain.RegistrationConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RegistrationExceptionHandler {
    @ExceptionHandler(InvalidCpfException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCpf(InvalidCpfException exception) {
        return build(HttpStatus.BAD_REQUEST, "Invalid CPF", exception.getMessage());
    }

    @ExceptionHandler(CpfAlreadyInUseException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateCpf(CpfAlreadyInUseException exception) {
        return build(HttpStatus.CONFLICT, "CPF already in use", exception.getMessage());
    }

    @ExceptionHandler(RegistrationConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(RegistrationConflictException exception) {
        return build(HttpStatus.CONFLICT, "Registration conflict", exception.getMessage());
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);

        return ResponseEntity.status(status).body(problem);
    }
}