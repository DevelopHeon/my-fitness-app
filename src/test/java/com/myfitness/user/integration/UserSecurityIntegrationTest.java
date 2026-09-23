package com.myfitness.user.integration;

import static com.myfitness.test.security.TestSecurity.authenticatedUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myfitness.user.application.port.out.UserRepositoryPort;
import com.myfitness.user.domain.model.User;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class UserSecurityIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepositoryPort userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedApiRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyUserIdHeaderCannotBypassAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("X-User-Id", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedPrincipalProvidesInternalUserId() throws Exception {
        User saved = userRepository.save(User.createFromGoogle(
                "google-subject-security",
                "security@example.com",
                "Security User",
                null,
                Instant.parse("2026-09-23T05:00:00Z")));

        mockMvc.perform(get("/api/users/me")
                        .with(authenticatedUser(saved.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.email")
                        .value("security@example.com"))
                .andExpect(jsonPath("$.displayName")
                        .value("Security User"));
    }


    @Test
    void authenticatedUserCanRequestCsrfToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf")
                        .with(authenticatedUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void logoutRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user("test-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedLogoutWithCsrfReturnsNoContent()
            throws Exception {
        mockMvc.perform(post("/logout")
                        .with(authenticatedUser(1L)))
                .andExpect(status().isNoContent());
    }
}
