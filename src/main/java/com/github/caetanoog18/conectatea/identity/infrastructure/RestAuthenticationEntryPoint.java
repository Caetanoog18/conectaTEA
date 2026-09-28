package com.github.caetanoog18.conectatea.identity.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final SecurityProblemResponseWriter responseWriter;

    public RestAuthenticationEntryPoint(SecurityProblemResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        responseWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                "A valid access token is required"
        );
    }
}