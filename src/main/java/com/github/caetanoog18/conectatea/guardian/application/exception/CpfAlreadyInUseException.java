package com.github.caetanoog18.conectatea.guardian.application.exception;

public class CpfAlreadyInUseException extends RuntimeException {
    public CpfAlreadyInUseException() {
        super("A guardian with this CPF already exists");
    }
}