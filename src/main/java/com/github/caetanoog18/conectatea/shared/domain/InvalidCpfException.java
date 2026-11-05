package com.github.caetanoog18.conectatea.shared.domain;

public class InvalidCpfException extends RuntimeException {
    public InvalidCpfException() {
        super("A valid CPF is required");
    }
}