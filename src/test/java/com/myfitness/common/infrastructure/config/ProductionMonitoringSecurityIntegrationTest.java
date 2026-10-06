package com.myfitness.common.infrastructure.config;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"prod", "monitoring"})
class ProductionMonitoringSecurityIntegrationTest extends MonitoringSecurityIntegrationTest {
}
