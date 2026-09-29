package com.myfitness.user.integration;

import static com.myfitness.test.security.TestSecurity.authenticatedUser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myfitness.user.application.port.out.UserRepositoryPort;
import com.myfitness.user.domain.model.User;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

@SpringBootTest
@Transactional
class UserSecurityIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepositoryPort userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void unauthenticatedApiRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void legacyUserIdHeaderCannotBypassAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me").header("X-User-Id", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedPrincipalProvidesInternalUserId() throws Exception {
        User saved =
                userRepository.save(
                        User.createFromGoogle(
                                "google-subject-security",
                                "security@example.com",
                                "Security User",
                                null,
                                Instant.parse("2026-09-23T05:00:00Z")));

        mockMvc.perform(get("/api/users/me").with(authenticatedUser(saved.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.email").value("security@example.com"))
                .andExpect(jsonPath("$.displayName").value("Security User"));
    }

    @Test
    void authenticatedUserCanBootstrapSpaCsrfToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.cookieName").value("XSRF-TOKEN"))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void logoutRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/logout").session(authenticatedSession()))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedLogoutWithSpaCsrfCookieReturnsNoContent() throws Exception {
        MockHttpSession session = authenticatedSession();
        MvcResult csrfResult =
                mockMvc.perform(get("/api/auth/csrf").session(session))
                        .andExpect(status().isOk())
                        .andReturn();

        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);

        mockMvc.perform(
                        post("/logout")
                                .session(session)
                                .cookie(csrfCookie)
                                .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());
    }

    private static MockHttpSession authenticatedSession() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "test-user", "credentials", List.of()));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }
}
