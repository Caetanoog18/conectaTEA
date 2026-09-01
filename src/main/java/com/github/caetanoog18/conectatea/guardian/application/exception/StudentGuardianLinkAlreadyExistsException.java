package com.github.caetanoog18.conectatea.guardian.application.exception;

public class StudentGuardianLinkAlreadyExistsException extends RuntimeException {
    public StudentGuardianLinkAlreadyExistsException() {
        super("Guardian is already linked to this student");
    }
}