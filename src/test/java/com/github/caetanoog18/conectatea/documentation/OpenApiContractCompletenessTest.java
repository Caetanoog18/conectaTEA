package com.github.caetanoog18.conectatea.documentation;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password="
})
@ActiveProfiles("docs")
@AutoConfigureMockMvc
class OpenApiContractCompletenessTest {
    private static final Set<String> HTTP_METHODS = Set.of(
            "get",
            "post",
            "put",
            "patch",
            "delete"
    );

    private static final Set<String> EXPECTED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/me",

            "/api/users",
            "/api/users/{userId}",
            "/api/users/{userId}/status",

            "/api/institution",

            "/api/students",
            "/api/students/{studentId}",
            "/api/students/{studentId}/status",

            "/api/guardians",
            "/api/guardians/{guardianId}",
            "/api/guardians/{guardianId}/status",
            "/api/guardians/{guardianId}/students",

            "/api/students/{studentId}/guardians",
            "/api/students/{studentId}/guardians/{guardianId}",

            "/api/student-guardian-links/{linkId}/consents",
            "/api/student-guardian-links/{linkId}/consents/active",
            "/api/consents/{consentId}",
            "/api/consents/{consentId}/revoke",

            "/api/students/{studentId}/care-team",
            "/api/care-team-links/{linkId}",
            "/api/care-team-links/{linkId}/end",

            "/api/me/students/{studentId}",
            "/api/me/students/{studentId}/observations",
            "/api/me/students/{studentId}/observations/{observationId}",
            "/api/me/students/{studentId}/timeline",

            "/api/audit-events",

            "/api/me/students/{studentId}/reports",
            "/api/me/students/{studentId}/reports/pdf"
    );

    private static final Set<String> EXPECTED_OPERATION_IDS = Set.of(
            "login",
            "getCurrentUser",

            "createUser",
            "listUsers",
            "getUser",
            "updateUserStatus",

            "createInstitution",
            "getInstitution",
            "updateInstitution",

            "createStudent",
            "listStudents",
            "getStudent",
            "updateStudent",
            "updateStudentStatus",

            "createGuardian",
            "listGuardians",
            "getGuardian",
            "updateGuardian",
            "updateGuardianStatus",

            "createStudentGuardianLink",
            "listStudentGuardians",
            "listGuardianStudents",
            "updateStudentGuardianLink",
            "deleteStudentGuardianLink",

            "createConsent",
            "getConsent",
            "getActiveConsent",
            "listConsentHistory",
            "revokeConsent",

            "createProfessionalLink",
            "listStudentCareTeam",
            "getProfessionalLink",
            "endProfessionalLink",

            "getAccessibleStudentProfile",

            "createObservation",
            "listAuthorizedObservations",
            "getAuthorizedObservation",

            "getAuthorizedStudentTimeline",
            "searchAuditEvents",

            "generateStudentReport",
            "exportStudentReportPdf"
    );

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeExactlyTheExpectedApiPaths() throws Exception {
        String specification = specification();

        Map<String, Object> paths = JsonPath.read(specification, "$.paths");

        assertThat(paths.keySet()).containsExactlyInAnyOrderElementsOf(EXPECTED_PATHS);
    }

    @Test
    void everyOperationShouldHaveUniqueIdTagAndResponses() throws Exception {
        String specification = specification();

        Map<String, Map<String, Object>> paths = JsonPath.read(specification, "$.paths");

        Set<String> operationIds = new HashSet<>();

        paths.forEach((path, pathItem) ->
                pathItem.forEach((method, rawOperation) -> {
                    if (!HTTP_METHODS.contains(method)) {
                        return;
                    }

                    assertThat(rawOperation)
                            .as("%s %s must be an operation", method, path)
                            .isInstanceOf(Map.class);

                    Map<?, ?> operation = (Map<?, ?>) rawOperation;

                    assertThat(operation.get("operationId"))
                            .as("%s %s must have operationId", method, path)
                            .isInstanceOf(String.class);

                    String operationId = (String) operation.get("operationId");

                    assertThat(operationId)
                            .as("%s %s operationId", method, path)
                            .isNotBlank();

                    assertThat(operationIds.add(operationId))
                            .as("operationId %s must be unique", operationId)
                            .isTrue();

                    assertThat(operation.get("tags"))
                            .as("%s %s must have tags", method, path)
                            .isInstanceOf(List.class);

                    assertThat((List<?>) operation.get("tags"))
                            .as("%s %s tags", method, path)
                            .isNotEmpty();

                    assertThat(operation.get("responses"))
                            .as("%s %s must have responses", method, path)
                            .isInstanceOf(Map.class);

                    assertThat((Map<?, ?>) operation.get("responses"))
                            .as("%s %s responses", method, path)
                            .isNotEmpty();
                })
        );

        assertThat(operationIds).containsExactlyInAnyOrderElementsOf(EXPECTED_OPERATION_IDS);
    }

    @Test
    void loginShouldBeTheOnlyPublicBusinessOperation() throws Exception {
        String specification = specification();

        List<?> globalSecurity = JsonPath.read(specification, "$.security");
        List<?> loginSecurity = JsonPath.read(specification, "$.paths['/api/auth/login'].post.security");

        assertThat(globalSecurity).isNotEmpty();
        assertThat(loginSecurity).isEmpty();
    }

    private String specification() throws Exception {
        return mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}