package com.github.caetanoog18.conectatea.careteam.api;

import com.github.caetanoog18.conectatea.careteam.application.exception.CareTeamConflictException;
import com.github.caetanoog18.conectatea.careteam.application.exception.CareTeamNotFoundException;
import com.github.caetanoog18.conectatea.careteam.application.exception.InvalidCareTeamLinkException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CareTeamExceptionHandler {
    @ExceptionHandler(CareTeamNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(CareTeamNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "Care team resource not found", exception.getMessage());
    }

    @ExceptionHandler(CareTeamConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(CareTeamConflictException exception) {
        return build(HttpStatus.CONFLICT, "Care team conflict", exception.getMessage());
    }

    @ExceptionHandler(InvalidCareTeamLinkException.class)
    public ResponseEntity<ProblemDetail> handleInvalid(InvalidCareTeamLinkException exception) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid care team link", exception.getMessage());
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);

        return ResponseEntity.status(status).body(problem);
    }
}