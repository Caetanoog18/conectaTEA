package com.github.caetanoog18.conectatea.shared.infrastructure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("docs")
public class OpenApiConfiguration {
    @Bean
    public OpenAPI conectaTeaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                                .title("ConectaTEA API")
                                .version("0.0.1")
                                .description("""
                                        API de acompanhamento educacional
                                        e multiprofissional de estudantes.

                                        O acesso depende da autenticação,
                                        do perfil atual, dos vínculos e dos
                                        consentimentos exigidos em cada operação.

                                        Utilize somente dados fictcios
                                        nos testes realizados pelo Swagger.
                                        """)
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearerAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "Informe somente o accessToken, " + "sem o prefixo Bearer.")
                                )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OpenApiCustomizer publicLoginDocumentation() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            var loginPath = openApi.getPaths().get("/api/auth/login");

            if (loginPath != null && loginPath.getPost() != null) {
                loginPath.getPost().setSecurity(List.of());
            }
        };
    }
}