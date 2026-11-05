package com.github.caetanoog18.conectatea.identity.application.exception;

public class EmailAlreadyInUseException extends RuntimeException{
    public EmailAlreadyInUseException() {
        super("A user with this email already exists");
    }
}
