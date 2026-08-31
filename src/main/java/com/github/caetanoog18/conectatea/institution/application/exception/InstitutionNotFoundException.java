package com.github.caetanoog18.conectatea.institution.application.exception;

public class InstitutionNotFoundException extends RuntimeException {
    public InstitutionNotFoundException() {
        super("The institution has not been registered");
    }
}