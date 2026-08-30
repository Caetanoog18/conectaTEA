package com.github.caetanoog18.conectatea.identity.application.exception;

public class SelfDeactivationException extends RuntimeException {
    public SelfDeactivationException() {
        super("An administrator cannot deactivate their own account");
    }
}
