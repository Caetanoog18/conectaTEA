package com.github.caetanoog18.conectatea.institution;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.institution.domain.Institution;
import com.github.caetanoog18.conectatea.institution.infrastructure.InstitutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class InstitutionManagementIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Test
    void administratorShouldCreateInstitution() throws Exception {
        mockMvc.perform(
                        post("/api/institution")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_ADMINISTRATOR"
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestBody())
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name")
                        .value("Escola ConectaTEA"))
                .andExpect(jsonPath("$.taxId")
                        .value("12345678000190"))
                .andExpect(jsonPath("$.email")
                        .value("contato@conectatea.com"))
                .andExpect(jsonPath("$.state").value("SC"))
                .andExpect(jsonPath("$.postalCode")
                        .value("89000000"));

        assertThat(institutionRepository.count()).isEqualTo(1);
    }

    @Test
    void teacherShouldNotCreateInstitution() throws Exception {
        mockMvc.perform(
                        post("/api/institution")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_TEACHER"
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestBody())
                )
                .andExpect(status().isForbidden());

        assertThat(institutionRepository.count()).isZero();
    }

    @Test
    void authenticatedUserShouldFindInstitution() throws Exception {
        persistInstitution();

        mockMvc.perform(
                        get("/api/institution")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_TEACHER"
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Escola ConectaTEA"))
                .andExpect(jsonPath("$.city").value("Blumenau"))
                .andExpect(jsonPath("$.state").value("SC"));
    }

    @Test
    void administratorShouldUpdateInstitution() throws Exception {
        persistInstitution();

        String updateRequest = """
                {
                  "name": "Escola ConectaTEA Atualizada",
                  "taxId": "12.345.678/0001-90",
                  "email": "novo@conectatea.com",
                  "phone": "(47) 99999-9999",
                  "street": "Rua XV de Novembro",
                  "addressNumber": "200",
                  "complement": "Bloco B",
                  "district": "Centro",
                  "city": "Blumenau",
                  "state": "sc",
                  "postalCode": "89010-000"
                }
                """;

        mockMvc.perform(
                        put("/api/institution")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_ADMINISTRATOR"
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Escola ConectaTEA Atualizada"))
                .andExpect(jsonPath("$.email")
                        .value("novo@conectatea.com"))
                .andExpect(jsonPath("$.phone")
                        .value("47999999999"))
                .andExpect(jsonPath("$.postalCode")
                        .value("89010000"));

        Institution updated = institutionRepository
                .findFirstByOrderByCreatedAtAsc()
                .orElseThrow();

        assertThat(updated.getName())
                .isEqualTo("Escola ConectaTEA Atualizada");
        assertThat(updated.getEmail())
                .isEqualTo("novo@conectatea.com");
    }

    @Test
    void secondInstitutionShouldBeRejected() throws Exception {
        persistInstitution();

        mockMvc.perform(
                        post("/api/institution")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_ADMINISTRATOR"
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestBody())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Institution already exists"));

        assertThat(institutionRepository.count()).isEqualTo(1);
    }

    @Test
    void missingInstitutionShouldReturnNotFound() throws Exception {
        mockMvc.perform(
                        get("/api/institution")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_TEACHER"
                                        )
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title")
                        .value("Institution not found"));
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/institution"))
                .andExpect(status().isUnauthorized());
    }

    private Institution persistInstitution() {
        Institution institution = new Institution(
                "Escola ConectaTEA",
                "12.345.678/0001-90",
                "contato@conectatea.com",
                "(47) 3333-4444",
                "Rua das Flores",
                "100",
                null,
                "Centro",
                "Blumenau",
                "SC",
                "89000-000"
        );

        return institutionRepository.saveAndFlush(institution);
    }

    private static String createRequestBody() {
        return """
                {
                  "name": "Escola ConectaTEA",
                  "taxId": "12.345.678/0001-90",
                  "email": "CONTATO@CONECTATEA.COM",
                  "phone": "(47) 3333-4444",
                  "street": "Rua das Flores",
                  "addressNumber": "100",
                  "complement": null,
                  "district": "Centro",
                  "city": "Blumenau",
                  "state": "sc",
                  "postalCode": "89000-000"
                }
                """;
    }
}