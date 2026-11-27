package com.myfitness.dashboard.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.myfitness.dashboard.application.port.in.DashboardQueryPort;
import com.myfitness.dashboard.presentation.dto.response.DashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardQueryPort dashboardQueryPort;

    public DashboardController(DashboardQueryPort dashboardQueryPort) {
        this.dashboardQueryPort = dashboardQueryPort;
    }

    @GetMapping
    public DashboardResponse get(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return DashboardResponse.from(
                dashboardQueryPort.getDashboard(userId));
    }
}
