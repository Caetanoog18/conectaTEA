package com.github.caetanoog18.conectatea.consent.api;

import com.github.caetanoog18.conectatea.consent.application.exception.ConsentConflictException;
import com.github.caetanoog18.conectatea.consent.application.exception.ConsentNotFoundException;
import com.github.caetanoog18.conectatea.consent.application.exception.InvalidConsentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ConsentExceptionHandler {
    @ExceptionHandler(ConsentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ConsentNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "Consent not found", exception.getMessage());
    }

    @ExceptionHandler(ConsentConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConsentConflictException exception) {
        return build(HttpStatus.CONFLICT, "Consent conflict", exception.getMessage());
    }

    @ExceptionHandler(InvalidConsentException.class)
    public ResponseEntity<ProblemDetail> handleInvalid(InvalidConsentException exception) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid consent", exception.getMessage());
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}