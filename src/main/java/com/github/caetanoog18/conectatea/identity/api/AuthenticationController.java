package com.github.caetanoog18.conectatea.identity.api;

import com.github.caetanoog18.conectatea.identity.api.dto.AuthenticatedUserResponse;
import com.github.caetanoog18.conectatea.identity.api.dto.LoginRequest;
import com.github.caetanoog18.conectatea.identity.api.dto.TokenResponse;
import com.github.caetanoog18.conectatea.identity.application.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(
            AuthenticationService authenticationService
    ) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
                authenticationService.login(request)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> currentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                new AuthenticatedUserResponse(
                        jwt.getSubject(),
                        jwt.getClaimAsStringList("roles")
                )
        );
    }
}