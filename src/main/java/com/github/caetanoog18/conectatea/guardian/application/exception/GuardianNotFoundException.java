package com.github.caetanoog18.conectatea.guardian.application.exception;

import java.util.UUID;

public class GuardianNotFoundException extends RuntimeException {
    public GuardianNotFoundException(UUID guardianId) {
        super("Guardian not found: " + guardianId);
    }
}