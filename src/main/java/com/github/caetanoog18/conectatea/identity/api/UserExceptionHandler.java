package com.github.caetanoog18.conectatea.identity.api;

import com.github.caetanoog18.conectatea.identity.application.exception.EmailAlreadyInUseException;
import com.github.caetanoog18.conectatea.identity.application.exception.SelfDeactivationException;
import com.github.caetanoog18.conectatea.identity.application.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEmail(
            EmailAlreadyInUseException exception
    ) {
        return buildProblem(
                HttpStatus.CONFLICT,
                "Email already in use",
                exception.getMessage()
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(
            UserNotFoundException exception
    ) {
        return buildProblem(
                HttpStatus.NOT_FOUND,
                "User not found",
                exception.getMessage()
        );
    }

    @ExceptionHandler(SelfDeactivationException.class)
    public ResponseEntity<ProblemDetail> handleSelfDeactivation(
            SelfDeactivationException exception
    ) {
        return buildProblem(
                HttpStatus.CONFLICT,
                "User status conflict",
                exception.getMessage()
        );
    }

    private ResponseEntity<ProblemDetail> buildProblem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail
        );

        problem.setTitle(title);

        return ResponseEntity
                .status(status)
                .body(problem);
    }
}