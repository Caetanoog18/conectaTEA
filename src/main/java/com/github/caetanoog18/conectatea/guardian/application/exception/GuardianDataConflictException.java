package com.github.caetanoog18.conectatea.guardian.application.exception;

public class GuardianDataConflictException extends RuntimeException {
    public GuardianDataConflictException() {
        super("Guardian data conflicts with an existing record");
    }
}