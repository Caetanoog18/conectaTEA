package com.github.caetanoog18.conectatea.identity.application.exception;

public class LoginRateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public LoginRateLimitExceededException(long retryAfterSeconds) {
        super("Too many authentication attempts");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}