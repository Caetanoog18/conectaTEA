package com.github.caetanoog18.conectatea.identity.application;

import com.github.caetanoog18.conectatea.identity.api.dto.LoginRequest;
import com.github.caetanoog18.conectatea.identity.api.dto.TokenResponse;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Locale;

@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            UserRepository userRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    public TokenResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        var authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, request.password());

        var authentication = authenticationManager.authenticate(authenticationRequest);

        var user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        return tokenService.generateToken(authentication, user.getTokenVersion());
    }
}