package com.myfitness.dashboard.application.port.in;

public interface DashboardQueryPort {
    DashboardView getDashboard(Long userId);
}
