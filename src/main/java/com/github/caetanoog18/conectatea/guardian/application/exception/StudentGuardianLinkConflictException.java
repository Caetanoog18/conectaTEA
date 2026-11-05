package com.github.caetanoog18.conectatea.guardian.application.exception;

public class StudentGuardianLinkConflictException extends RuntimeException {
    public StudentGuardianLinkConflictException() {
        super("Student and guardian link conflicts with an existing record");
    }
}