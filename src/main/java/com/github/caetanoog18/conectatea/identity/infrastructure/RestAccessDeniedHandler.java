package com.github.caetanoog18.conectatea.identity.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityProblemResponseWriter responseWriter;

    public RestAccessDeniedHandler(SecurityProblemResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        responseWriter.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "Access denied",
                "You do not have permission to access this resource"
        );
    }
}