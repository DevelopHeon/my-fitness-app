package com.myfitness.common.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

class MonitoringConfigurationTest {
    @Test
    void missingPasswordFailsStartup() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("monitoring");
            context.registerBean(HttpSecurity.class, () -> mock(HttpSecurity.class));
            context.register(MonitoringSecurityConfig.class);
            assertThatThrownBy(context::refresh).hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasStackTraceContaining("monitoring requires app.monitoring.password");
        }
    }
}
