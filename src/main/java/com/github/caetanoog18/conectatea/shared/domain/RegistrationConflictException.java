package com.github.caetanoog18.conectatea.shared.domain;

public class RegistrationConflictException extends RuntimeException {
    public RegistrationConflictException() {
        super("Registration conflicts with existing data");
    }
}