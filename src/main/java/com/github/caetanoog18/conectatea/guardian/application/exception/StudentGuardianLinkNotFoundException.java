package com.github.caetanoog18.conectatea.guardian.application.exception;

public class StudentGuardianLinkNotFoundException extends RuntimeException {
    public StudentGuardianLinkNotFoundException() {
        super("Guardian is not linked to this student");
    }
}