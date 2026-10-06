package com.myfitness.common.infrastructure.config;

import static com.myfitness.test.security.TestSecurity.authenticatedUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "app.monitoring.password=test-monitoring-password")
@ActiveProfiles("monitoring")
@AutoConfigureMetrics
class MonitoringSecurityIntegrationTest {
    @Autowired WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void requiresDedicatedCredentialsAndDoesNotTrustSessionUsers() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").with(authenticatedUser(1L)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").with(httpBasic("prometheus", "wrong")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").with(httpBasic("prometheus", "test-monitoring-password")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")));
    }

    @Test
    void monitoringUserCannotAuthenticateBusinessApis() throws Exception {
        mvc.perform(get("/api/users/me").with(httpBasic("prometheus", "test-monitoring-password")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
