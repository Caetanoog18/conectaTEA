package com.github.caetanoog18.conectatea.institution;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.github.caetanoog18.conectatea.institution.domain.Institution;
import com.github.caetanoog18.conectatea.institution.infrastructure.InstitutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@Transactional
class InstitutionRepositoryIntegrationTest {
    @Autowired
    private InstitutionRepository institutionRepository;

    @Test
    void shouldPersistInstitution() {
        Institution institution = new Institution(
                "Escola ConectaTEA",
                "12.345.678/0001-90",
                "CONTATO@CONECTATEA.COM",
                "(47) 3333-4444",
                "Rua das Flores",
                "100",
                null,
                "Centro",
                "Blumenau",
                "sc",
                "89000-000"
        );

        Institution savedInstitution = institutionRepository.saveAndFlush(institution);
        assertThat(savedInstitution.getId()).isNotNull();
        assertThat(savedInstitution.getName())
                .isEqualTo("Escola ConectaTEA");
        assertThat(savedInstitution.getEmail())
                .isEqualTo("contato@conectatea.com");
        assertThat(savedInstitution.getTaxId())
                .isEqualTo("12345678000190");
        assertThat(savedInstitution.getState()).isEqualTo("SC");
        assertThat(savedInstitution.getPostalCode())
                .isEqualTo("89000000");
        assertThat(
                institutionRepository.findById(savedInstitution.getId())
        ).contains(savedInstitution);
    }
}