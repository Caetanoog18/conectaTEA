package com.github.caetanoog18.conectatea.identity;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.security.jwt.issuer=conectatea-api",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@AutoConfigureMockMvc
@Transactional
class CurrentUserJwtIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void activeAccountWithMatchingRoleShouldBeAccepted() throws Exception {
        User user = createUser();

        mockMvc.perform(
                get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(user.getEmail(), "TEACHER")))
                .andExpect(status().isOk());
    }

    @Test
    void previouslyIssuedTokenShouldBeRejectedAfterDeactivation() throws Exception {
        User user = createUser();
        String accessToken = token(user.getEmail(), "TEACHER");

        user.deactivate();
        userRepository.saveAndFlush(user);

        mockMvc.perform(
                get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenRoleDifferentFromDatabaseShouldBeRejected() throws Exception {
        User user = createUser();

        mockMvc.perform(
                get("/api/auth/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token(
                                        user.getEmail(),
                                        "ADMINISTRATOR"
                                )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenForUnknownAccountShouldBeRejected() throws Exception {
        mockMvc.perform(
                get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(
                                UUID.randomUUID() + "@example.com",
                                "TEACHER")))
                .andExpect(status().isUnauthorized());
    }

    private User createUser() {
        return userRepository.saveAndFlush(
                new User(
                        "Professor Teste JWT",
                        UUID.randomUUID() + "@example.com",
                        "unused-password-hash",
                        UserRole.TEACHER
                )
        );
    }

    private String token(String email, String role) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("conectatea-api")
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("roles", List.of(role))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}