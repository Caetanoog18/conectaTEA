package com.github.caetanoog18.conectatea.identity;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@AutoConfigureMockMvc
@Transactional
class UserManagementIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void administratorShouldCreateUser() throws Exception {
        String requestBody = """
                {
                  "fullName": "Maria da Silva",
                  "email": "maria@conectatea.com",
                  "password": "StrongPassword123!",
                  "role": "TEACHER"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_ADMINISTRATOR"
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email")
                        .value("maria@conectatea.com"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        var savedUser = userRepository
                .findByEmailIgnoreCase("maria@conectatea.com")
                .orElseThrow();

        assertThat(savedUser.getPasswordHash())
                .isNotEqualTo("StrongPassword123!");

        assertThat(
                passwordEncoder.matches(
                        "StrongPassword123!",
                        savedUser.getPasswordHash()
                )
        ).isTrue();
    }

    @Test
    void teacherShouldNotCreateUser() throws Exception {
        String requestBody = """
                {
                  "fullName": "Maria da Silva",
                  "email": "maria@conectatea.com",
                  "password": "StrongPassword123!",
                  "role": "TEACHER"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_TEACHER"
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        String requestBody = """
                {
                  "fullName": "Maria da Silva",
                  "email": "maria@conectatea.com",
                  "password": "StrongPassword123!",
                  "role": "TEACHER"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized());
    }
}