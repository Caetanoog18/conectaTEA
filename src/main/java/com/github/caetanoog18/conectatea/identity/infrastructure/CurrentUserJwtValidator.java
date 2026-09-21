package com.github.caetanoog18.conectatea.identity.infrastructure;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CurrentUserJwtValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error(
            "invalid_token",
            "Token is not valid for the current account",
            null
    );

    private final UserRepository userRepository;

    public CurrentUserJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String subject = token.getSubject();

        if (subject == null || subject.isBlank()) {
            return rejected();
        }

        Object rolesClaim = token.getClaims().get("roles");

        if (!(rolesClaim instanceof List<?> roles) || roles.size() != 1) {
            return rejected();
        }

        var user = userRepository.findByEmailIgnoreCase(subject);

        if (user.isEmpty() || !user.get().isActive()) {
            return rejected();
        }

        String currentRole = user.get().getRole().name();

        if (!currentRole.equals(roles.get(0))) {
            return rejected();
        }

        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult rejected() {
        return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
    }
}