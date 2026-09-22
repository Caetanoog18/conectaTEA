package com.github.caetanoog18.conectatea.identity.application;

import com.github.caetanoog18.conectatea.identity.api.dto.LoginRequest;
import com.github.caetanoog18.conectatea.identity.api.dto.TokenResponse;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;

import java.util.Locale;

@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            UserRepository userRepository,
            LoginAttemptService loginAttemptService
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
    }

    public TokenResponse login(LoginRequest request, String clientAddress) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        loginAttemptService.checkAllowed(normalizedEmail, clientAddress);

        var authenticationRequest = UsernamePasswordAuthenticationToken
                .unauthenticated(normalizedEmail, request.password());

        final Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(authenticationRequest);
        } catch (AuthenticationException exception) {
            loginAttemptService.recordFailure(normalizedEmail, clientAddress);
            loginAttemptService.checkAllowed(normalizedEmail, clientAddress);

            throw exception;
        }

        var user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        loginAttemptService.recordSuccess(normalizedEmail, clientAddress);

        return tokenService.generateToken(authentication, user.getTokenVersion());
    }
}