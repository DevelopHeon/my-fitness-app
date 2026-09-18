package com.myfitness.dashboard.controller;

import com.myfitness.dashboard.dto.response.DashboardResponse;
import com.myfitness.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse get(
            @RequestHeader("X-User-Id") Long userId) {
        return dashboardService.getDashboard(userId);
    }
}
