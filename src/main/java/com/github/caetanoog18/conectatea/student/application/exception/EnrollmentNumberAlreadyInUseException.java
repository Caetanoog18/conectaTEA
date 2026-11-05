package com.github.caetanoog18.conectatea.student.application.exception;

public class EnrollmentNumberAlreadyInUseException extends RuntimeException {
    public EnrollmentNumberAlreadyInUseException() {

        super("A student with this enrollment number already exists");
    }
}