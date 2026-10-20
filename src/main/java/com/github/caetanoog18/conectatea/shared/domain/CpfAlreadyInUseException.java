package com.github.caetanoog18.conectatea.shared.domain;

public class CpfAlreadyInUseException extends RuntimeException {
    public CpfAlreadyInUseException() {
        super("CPF already in use");
    }
}