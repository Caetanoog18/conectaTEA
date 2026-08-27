package com.github.caetanoog18.conectatea.identity;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@Transactional
class UserRepositoryIntegrationTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmailIgnoringCase() {
        User user = new User(
                "System Administrator",
                "admin@conectatea.com",
                "temporary-password-hash",
                UserRole.ADMINISTRATOR
        );

        userRepository.saveAndFlush(user);

        assertThat(
                userRepository.findByEmailIgnoreCase("ADMIN@CONECTATEA.COM")
        ).contains(user);
    }
}