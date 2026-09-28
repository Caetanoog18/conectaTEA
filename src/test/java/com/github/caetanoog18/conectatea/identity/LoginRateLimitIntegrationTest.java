package com.github.caetanoog18.conectatea.identity;

import com.github.caetanoog18.conectatea.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.security.jwt.secret=" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.bootstrap.admin.email=",
        "app.bootstrap.admin.password=",
        "app.security.login.max-attempts=3",
        "app.security.login.window=PT10M",
        "app.security.login.block-duration=PT15M"
})
@AutoConfigureMockMvc
class LoginRateLimitIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void thirdInvalidLoginShouldBeRateLimited() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String body = """
                {
                  "email": "%s",
                  "password": "InvalidPassword123!"
                }
                """.formatted(email);

        mockMvc.perform(login(body)).andExpect(status().isUnauthorized());
        mockMvc.perform(login(body)).andExpect(status().isUnauthorized());
        mockMvc.perform(login(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, not(blankOrNullString())))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too many requests"));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(String body) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body);
    }
}