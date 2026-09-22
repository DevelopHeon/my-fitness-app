package com.myfitness.dashboard.presentation.controller;

import com.myfitness.dashboard.application.port.in.DashboardQueryUseCase;
import com.myfitness.dashboard.presentation.dto.response.DashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardQueryUseCase dashboardQueryUseCase;

    public DashboardController(DashboardQueryUseCase dashboardQueryUseCase) {
        this.dashboardQueryUseCase = dashboardQueryUseCase;
    }

    @GetMapping
    public DashboardResponse get(
            @RequestHeader("X-User-Id") Long userId) {
        return DashboardResponse.from(
                dashboardQueryUseCase.getDashboard(userId));
    }
}
