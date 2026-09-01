package com.github.caetanoog18.conectatea.guardian.application.exception;

public class PrimaryContactAlreadyExistsException extends RuntimeException {
    public PrimaryContactAlreadyExistsException() {
        super("Student already has a primary contact");
    }
}