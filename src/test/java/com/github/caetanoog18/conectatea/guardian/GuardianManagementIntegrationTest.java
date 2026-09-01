package com.github.caetanoog18.conectatea.guardian;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.guardian.domain.Guardian;
import com.github.caetanoog18.conectatea.guardian.infrastructure.GuardianRepository;
import com.github.caetanoog18.conectatea.identity.domain.User;
import com.github.caetanoog18.conectatea.identity.domain.UserRole;
import com.github.caetanoog18.conectatea.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class GuardianManagementIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void administratorShouldCreateGuardian() throws Exception {
        mockMvc.perform(
                        post("/api/guardians")
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(guardianRequestBody(null))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fullName")
                        .value("Maria da Silva"))
                .andExpect(jsonPath("$.cpf")
                        .value("52998224725"))
                .andExpect(jsonPath("$.email")
                        .value("maria@example.com"))
                .andExpect(jsonPath("$.phone")
                        .value("47999999999"))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(guardianRepository.count()).isEqualTo(1);
    }

    @Test
    void coordinatorShouldCreateGuardian() throws Exception {
        mockMvc.perform(
                        post("/api/guardians")
                                .with(withRole(
                                        "PEDAGOGICAL_COORDINATOR"
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(guardianRequestBody(null))
                )
                .andExpect(status().isCreated());

        assertThat(guardianRepository.count()).isEqualTo(1);
    }

    @Test
    void teacherShouldNotManageGuardians() throws Exception {
        mockMvc.perform(
                        post("/api/guardians")
                                .with(withRole("TEACHER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(guardianRequestBody(null))
                )
                .andExpect(status().isForbidden());

        assertThat(guardianRepository.count()).isZero();
    }

    @Test
    void administratorShouldListAndFindGuardian() throws Exception {
        Guardian guardian = persistGuardian(null);

        mockMvc.perform(
                        get("/api/guardians")
                                .with(withRole("ADMINISTRATOR"))
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName")
                        .value("Maria da Silva"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(
                        get(
                                "/api/guardians/{guardianId}",
                                guardian.getId()
                        )
                                .with(withRole("ADMINISTRATOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(guardian.getId().toString()))
                .andExpect(jsonPath("$.cpf")
                        .value("52998224725"));
    }

    @Test
    void administratorShouldUpdateAndDeactivateGuardian() throws Exception {
        Guardian guardian = persistGuardian(null);
        String updateBody = """
                {
                  "fullName": "Maria Oliveira da Silva",
                  "cpf": "529.982.247-25",
                  "email": "nova@example.com",
                  "phone": "(47) 98888-7777",
                  "userId": null
                }
                """;

        mockMvc.perform(
                        put(
                                "/api/guardians/{guardianId}",
                                guardian.getId()
                        )
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName")
                        .value("Maria Oliveira da Silva"))
                .andExpect(jsonPath("$.email")
                        .value("nova@example.com"))
                .andExpect(jsonPath("$.phone")
                        .value("47988887777"));

        mockMvc.perform(
                        patch(
                                "/api/guardians/{guardianId}/status",
                                guardian.getId()
                        )
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "active": false
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        Guardian updated = guardianRepository
                .findById(guardian.getId())
                .orElseThrow();

        assertThat(updated.getFullName())
                .isEqualTo("Maria Oliveira da Silva");
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void duplicateCpfShouldBeRejected() throws Exception {
        persistGuardian(null);

        mockMvc.perform(
                        post("/api/guardians")
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(guardianRequestBody(null))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("CPF already in use"));

        assertThat(guardianRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldLinkLegalGuardianUser() throws Exception {
        User user = userRepository.saveAndFlush(
                new User(
                        "Maria da Silva",
                        "maria.guardian@example.com",
                        "temporary-password-hash",
                        UserRole.LEGAL_GUARDIAN
                )
        );

        mockMvc.perform(
                        post("/api/guardians")
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        guardianRequestBody(user.getId())
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId")
                        .value(user.getId().toString()));

        Guardian savedGuardian = guardianRepository
                .findByCpf("52998224725")
                .orElseThrow();

        assertThat(savedGuardian.getUserId())
                .isEqualTo(user.getId());
    }

    @Test
    void nonGuardianUserShouldBeRejected() throws Exception {
        User teacher = userRepository.saveAndFlush(
                new User(
                        "Professor da Silva",
                        "teacher@example.com",
                        "temporary-password-hash",
                        UserRole.TEACHER
                )
        );

        mockMvc.perform(
                        post("/api/guardians")
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        guardianRequestBody(
                                                teacher.getId()
                                        )
                                )
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title")
                        .value("Invalid guardian user"));

        assertThat(guardianRepository.count()).isZero();
    }

    @Test
    void missingGuardianShouldReturnNotFound() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/guardians/{guardianId}",
                                UUID.randomUUID()
                        )
                                .with(withRole("ADMINISTRATOR"))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title")
                        .value("Guardian not found"));
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/guardians"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void statusUpdateWithoutActiveShouldBeRejected() throws Exception {
        mockMvc.perform(
                        patch(
                                "/api/guardians/{guardianId}/status",
                                UUID.randomUUID()
                        )
                                .with(withRole("ADMINISTRATOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());
    }

    private Guardian persistGuardian(UUID userId) {
        Guardian guardian = new Guardian(
                "Maria da Silva",
                "529.982.247-25",
                "MARIA@EXAMPLE.COM",
                "(47) 99999-9999",
                userId
        );
        return guardianRepository.saveAndFlush(guardian);
    }

    private static String guardianRequestBody(UUID userId) {
        String userIdJson = userId == null
                ? "null"
                : "\"" + userId + "\"";

        return """
                {
                  "fullName": "Maria da Silva",
                  "cpf": "529.982.247-25",
                  "email": "MARIA@EXAMPLE.COM",
                  "phone": "(47) 99999-9999",
                  "userId": %s
                }
                """.formatted(userIdJson);
    }

    private static RequestPostProcessor withRole(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}