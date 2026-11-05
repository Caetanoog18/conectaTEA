package com.github.caetanoog18.conectatea.institution.api;

import com.github.caetanoog18.conectatea.institution.application.exception.InstitutionAlreadyExistsException;
import com.github.caetanoog18.conectatea.institution.application.exception.InstitutionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InstitutionExceptionHandler {
    @ExceptionHandler(InstitutionAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleAlreadyExists(
            InstitutionAlreadyExistsException exception
    ) {
        return buildProblem(
                HttpStatus.CONFLICT,
                "Institution already exists",
                exception.getMessage()
        );
    }

    @ExceptionHandler(InstitutionNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            InstitutionNotFoundException exception
    ) {
        return buildProblem(
                HttpStatus.NOT_FOUND,
                "Institution not found",
                exception.getMessage()
        );
    }

    private ResponseEntity<ProblemDetail> buildProblem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);

        return ResponseEntity
                .status(status)
                .body(problem);
    }
}