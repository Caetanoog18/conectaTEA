package com.github.caetanoog18.conectatea.identity.application;

import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitialAdministratorInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String administratorName;
    private final String administratorEmail;
    private final String administratorPassword;

    public InitialAdministratorInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.name:System Administrator}")
            String administratorName,
            @Value("${app.bootstrap.admin.email:}")
            String administratorEmail,
            @Value("${app.bootstrap.admin.password:}")
            String administratorPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.administratorName = administratorName;
        this.administratorEmail = administratorEmail;
        this.administratorPassword = administratorPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        boolean emailMissing = administratorEmail.isBlank();
        boolean passwordMissing = administratorPassword.isBlank();

        if (emailMissing && passwordMissing) {
            return;
        }

        if (emailMissing || passwordMissing) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_EMAIL and INITIAL_ADMIN_PASSWORD " +
                            "must be provided together"
            );
        }

        if (administratorPassword.length() < 12) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_PASSWORD must contain at least 12 characters"
            );
        }

        if (userRepository.existsByEmailIgnoreCase(administratorEmail)) {
            return;
        }

        User administrator = new User(
                administratorName,
                administratorEmail,
                passwordEncoder.encode(administratorPassword),
                UserRole.ADMINISTRATOR
        );

        userRepository.save(administrator);
    }
}