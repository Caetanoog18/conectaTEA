package com.github.caetanoog18.conectatea.guardian.api;

import com.github.caetanoog18.conectatea.guardian.application.exception.CpfAlreadyInUseException;
import com.github.caetanoog18.conectatea.guardian.application.exception.GuardianDataConflictException;
import com.github.caetanoog18.conectatea.guardian.application.exception.GuardianNotFoundException;
import com.github.caetanoog18.conectatea.guardian.application.exception.GuardianUserAlreadyLinkedException;
import com.github.caetanoog18.conectatea.guardian.application.exception.InvalidGuardianUserException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GuardianExceptionHandler {
    @ExceptionHandler(GuardianNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(GuardianNotFoundException exception) {
        return build(
                HttpStatus.NOT_FOUND,
                "Guardian not found",
                exception.getMessage()
        );
    }

    @ExceptionHandler(CpfAlreadyInUseException.class)
    public ResponseEntity<ProblemDetail> handleCpfConflict(CpfAlreadyInUseException exception) {
        return build(
                HttpStatus.CONFLICT,
                "CPF already in use",
                exception.getMessage()
        );
    }

    @ExceptionHandler(GuardianUserAlreadyLinkedException.class)
    public ResponseEntity<ProblemDetail> handleUserConflict(GuardianUserAlreadyLinkedException exception) {
        return build(HttpStatus.CONFLICT, "Guardian user already linked", exception.getMessage()
        );
    }

    @ExceptionHandler(GuardianDataConflictException.class)
    public ResponseEntity<ProblemDetail> handleDataConflict(GuardianDataConflictException exception) {
        return build(
                HttpStatus.CONFLICT,
                "Guardian data conflict",
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidGuardianUserException.class)
    public ResponseEntity<ProblemDetail> handleInvalidUser(InvalidGuardianUserException exception) {
        return build(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Invalid guardian user",
                exception.getMessage()
        );
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}