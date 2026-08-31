package com.github.caetanoog18.conectatea.student.application.exception;

import java.util.UUID;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(UUID studentId) {
        super("Student not found: " + studentId);
    }
}