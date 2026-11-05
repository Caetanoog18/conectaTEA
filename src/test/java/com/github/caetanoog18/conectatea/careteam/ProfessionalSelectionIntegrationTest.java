package com.github.caetanoog18.conectatea.careteam;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import com.github.caetanoog18.conectatea.support.TestCpf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password=",
        "app.bootstrap.admin.cpf="
})
@AutoConfigureMockMvc
@Transactional
class ProfessionalSelectionIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMINISTRATOR", "PEDAGOGICAL_COORDINATOR"})
    void managerShouldListOnlyActiveEligibleProfessionals(UserRole managerRole) throws Exception {
        User manager = persistUser("Gestor", "manager@example.com", managerRole);
        User teacher = persistUser("Professor", "teacher@example.com", UserRole.TEACHER);
        User guardian = persistUser("Responsável", "guardian@example.com", UserRole.LEGAL_GUARDIAN);
        User inactive = persistUser("Médico", "doctor@example.com", UserRole.PHYSICIAN);

        inactive.deactivate();
        userRepository.saveAndFlush(inactive);

        mockMvc.perform(
                        get("/api/care-team/professionals")
                                .with(jwt()
                                        .jwt(token -> token.subject(manager.getEmail()))
                                        .authorities(new SimpleGrantedAuthority("ROLE_" + managerRole.name()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(hasItem(teacher.getId().toString())))
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem(guardian.getId().toString()))))
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem(inactive.getId().toString()))))
                .andExpect(jsonPath("$.content[0].cpf").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void currentDatabaseRoleShouldOverrideAdminToken() throws Exception {
        User teacher = persistUser("Professor", "teacher@example.com", UserRole.TEACHER);

        mockMvc.perform(
                        get("/api/care-team/professionals")
                                .with(jwt()
                                        .jwt(token -> token.subject(teacher.getEmail()))
                                        .authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/care-team/professionals")).andExpect(status().isUnauthorized());
    }

    private User persistUser(String name, String email, UserRole role) {
        return userRepository.saveAndFlush(
                new User(
                        name,
                        email,
                        "temporary-password-hash",
                        role,
                        TestCpf.next()
                )
        );
    }
}