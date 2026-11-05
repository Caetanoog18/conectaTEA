package com.github.caetanoog18.conectatea.identity.application;

import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.shared.domain.Cpf;
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
    private final String administratorCpf;

    public InitialAdministratorInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.name:System Administrator}")
            String administratorName,
            @Value("${app.bootstrap.admin.email:}")
            String administratorEmail,
            @Value("${app.bootstrap.admin.password:}")
            String administratorPassword,
            @Value("${app.bootstrap.admin.cpf:}")
            String administratorCpf
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.administratorName = administratorName;
        this.administratorEmail = administratorEmail;
        this.administratorPassword = administratorPassword;
        this.administratorCpf = administratorCpf;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (administratorEmail.isBlank() && administratorPassword.isBlank() && administratorCpf.isBlank()) {
            return;
        }

        if (administratorEmail.isBlank()) {
            throw new IllegalStateException("INITIAL_ADMIN_EMAIL must be provided");
        }

        if (userRepository.existsByEmailIgnoreCase(administratorEmail)) {
            return;
        }

        if (administratorPassword.length() < 12 || administratorPassword.length() > 64) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must contain between 12 and 64 characters");
        }

        if (administratorName.isBlank()) {
            throw new IllegalStateException("INITIAL_ADMIN_NAME must not be blank");
        }

        String cpf = Cpf.normalize(administratorCpf);

        if (userRepository.existsByCpf(cpf)) {
            throw new IllegalStateException("Initial administrator CPF already in use");
        }

        User administrator = new User(
                administratorName.trim(),
                administratorEmail,
                passwordEncoder.encode(administratorPassword),
                UserRole.ADMINISTRATOR,
                cpf
        );

        userRepository.saveAndFlush(administrator);
    }
}