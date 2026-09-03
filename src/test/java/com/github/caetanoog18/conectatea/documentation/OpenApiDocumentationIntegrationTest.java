package com.github.caetanoog18.conectatea.documentation;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@ActiveProfiles("docs")
@AutoConfigureMockMvc
class OpenApiDocumentationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeSpecificationWithBearerAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ConectaTEA API"))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath(
                        "$.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/users']").exists())
                .andExpect(jsonPath("$.paths['/api/me/students/{studentId}/reports/pdf']").exists());
    }

    @Test
    void loginShouldNotRequireBearerInSpecification() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty());
    }

    @Test
    void shouldExposeSwaggerInterface() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void documentationShouldNotMakeBusinessEndpointsPublic() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDocumentPasswordAsWriteOnly() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.LoginRequest.properties.password.writeOnly")
                        .value(true))
                .andExpect(jsonPath("$.components.schemas.LoginRequest.properties.password.format")
                        .value("password"));
    }

    @Test
    void shouldDocumentLoginSuccessAndAuthenticationFailure() throws Exception {
        String path = "$.paths['/api/auth/login'].post";

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path + ".operationId").value("login"))
                .andExpect(jsonPath(path + ".security").isEmpty())
                .andExpect(jsonPath(path + ".responses['200'].content['application/json']").exists())
                .andExpect(jsonPath(path + ".responses['401'].content['application/problem+json']").exists());
    }

    @Test
    void shouldDocumentJsonReportAndLimits() throws Exception {
        String path = "$.paths['/api/me/students/{studentId}/reports'].post";

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path + ".operationId").value("generateStudentReport"))
                .andExpect(jsonPath(path + ".responses['200'].content['application/json']").exists())
                .andExpect(jsonPath(path + ".responses['400']").exists())
                .andExpect(jsonPath(path + ".responses['401']").exists())
                .andExpect(jsonPath(path + ".responses['403']").exists())
                .andExpect(jsonPath(path + ".responses['422']").exists())
                .andExpect(jsonPath(path + ".responses['503']").exists());
    }

    @Test
    void shouldDocumentPdfAsBinaryWithDownloadHeaders() throws Exception {
        String path = "$.paths['/api/me/students/{studentId}/reports/pdf'].post";
        String success = path + ".responses['200']";

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path + ".operationId").value("exportStudentReportPdf"))
                .andExpect(jsonPath(success + ".content['application/pdf'].schema.type")
                        .value("string"))
                .andExpect(jsonPath(success + ".content['application/pdf'].schema.format")
                        .value("binary"))
                .andExpect(jsonPath(success + ".headers['Content-Disposition']").exists())
                .andExpect(jsonPath(success + ".headers['X-Report-ID']").exists())
                .andExpect(jsonPath(success + ".headers['X-Request-ID']").exists())
                .andExpect(jsonPath(path + ".responses['403']").exists())
                .andExpect(jsonPath(path + ".responses['422']").exists())
                .andExpect(jsonPath(path + ".responses['500']").exists())
                .andExpect(jsonPath(path + ".responses['503']").exists());
    }

    @Test
    void shouldDocumentReportPeriodAsDateTime() throws Exception {
        String schema = "$.components.schemas.GenerateStudentReportRequest.properties";

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(schema + ".from.format").value("date-time"))
                .andExpect(jsonPath(schema + ".to.format").value("date-time"));
    }

    @Test
    void shouldDocumentUserCreationAndStatusConflict() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/users'].post.operationId").value("createUser"))
                .andExpect(jsonPath(
                        "$.paths['/api/users'].post.responses['201']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/users'].post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/users'].post.responses['409']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/users'].post.responses['403']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/users/{userId}/status'].patch.responses['409']").exists());
    }

    @Test
    void shouldDocumentSingleInstitution() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/institution'].get.operationId").value("getInstitution"))
                .andExpect(jsonPath(
                        "$.paths['/api/institution'].get.responses['404']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/institution'].post.responses['201']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/institution'].post.responses['409']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/institution'].put.responses['403']").exists());
    }

    @Test
    void shouldDocumentStudentManagement() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/students'].get.operationId").value("listStudents"))
                .andExpect(jsonPath(
                        "$.paths['/api/students'].get.responses['200'].content['application/json'].schema")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/students'].post.responses['201']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/students/{studentId}'].put.responses['409']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/students/{studentId}/status'].patch.responses['404']").exists());
    }

    @Test
    void shouldDocumentPaginationParameters() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/users'].get.parameters[?(@.name == 'page')].schema.default")
                        .value(org.hamcrest.Matchers.hasItem(0)))
                .andExpect(jsonPath(
                        "$.paths['/api/users'].get.parameters[?(@.name == 'size')].schema.maximum")
                        .value(org.hamcrest.Matchers.hasItem(100)))
                .andExpect(jsonPath(
                        "$.paths['/api/students'].get.parameters[?(@.name == 'size')].schema.maximum")
                        .value(org.hamcrest.Matchers.hasItem(100)));
    }

    @Test
    void shouldDocumentUserPasswordConstraints() throws Exception {
        String password = "$.components.schemas.CreateUserRequest.properties.password";

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(password + ".writeOnly").value(true))
                .andExpect(jsonPath(password + ".minLength").value(12))
                .andExpect(jsonPath(password + ".maxLength").value(64));
    }

    @Test
    void invalidManagementPaginationShouldReturnBadRequest() throws Exception {
        for (String path : new String[]{"/api/users", "/api/students"}) {
            mockMvc.perform(
                            get(path)
                                    .param("page", "-1")
                                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(
                            get(path)
                                    .param("size", "0")
                                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(
                            get(path)
                                    .param("size", "101")
                                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                    .andExpect(status().isBadRequest());
        }
    }
}