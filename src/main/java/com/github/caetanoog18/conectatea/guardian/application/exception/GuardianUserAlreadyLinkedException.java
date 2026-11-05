package com.github.caetanoog18.conectatea.guardian.application.exception;

public class GuardianUserAlreadyLinkedException extends RuntimeException {
    public GuardianUserAlreadyLinkedException() {
        super("This account is already linked to a guardian");
    }
}