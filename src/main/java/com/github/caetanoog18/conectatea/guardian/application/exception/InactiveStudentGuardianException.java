package com.github.caetanoog18.conectatea.guardian.application.exception;

public class InactiveStudentGuardianException extends RuntimeException {
    public InactiveStudentGuardianException() {
        super("Student and guardian must be active to create a link");
    }
}