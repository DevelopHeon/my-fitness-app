package com.myfitness.dashboard.application.usecase;

import com.myfitness.dashboard.application.usecase.response.DashboardResult;

public interface DashboardQueryUseCase {
    DashboardResult getDashboard(Long userId);
}
