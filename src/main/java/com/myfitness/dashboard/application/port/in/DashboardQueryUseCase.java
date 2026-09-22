package com.myfitness.dashboard.application.port.in;

import com.myfitness.dashboard.application.result.DashboardResult;

public interface DashboardQueryUseCase {
    DashboardResult getDashboard(Long userId);
}
