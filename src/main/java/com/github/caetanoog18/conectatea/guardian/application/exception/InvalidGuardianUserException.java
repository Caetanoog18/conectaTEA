package com.github.caetanoog18.conectatea.guardian.application.exception;

public class InvalidGuardianUserException extends RuntimeException {
    public InvalidGuardianUserException() {
        super("Guardian user must exist and have the LEGAL_GUARDIAN role");
    }
}