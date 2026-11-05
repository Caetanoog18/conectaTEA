package com.github.caetanoog18.conectatea.institution.application.exception;

public class InstitutionAlreadyExistsException extends RuntimeException {
    public InstitutionAlreadyExistsException() {
        super("The institution has already been registered");
    }
}