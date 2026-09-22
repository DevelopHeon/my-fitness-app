package com.myfitness.dashboard.application.service;

import com.myfitness.dashboard.application.port.in.DashboardQueryUseCase;
import com.myfitness.dashboard.application.port.out.DashboardDataPort;
import com.myfitness.dashboard.application.result.DashboardResult;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService implements DashboardQueryUseCase {
    private final DashboardDataPort dashboardDataPort;
    private final DashboardResultAssembler resultAssembler;
    private final Clock clock;

    @Autowired
    public DashboardService(
            DashboardDataPort dashboardDataPort,
            DashboardResultAssembler resultAssembler) {
        this(
                dashboardDataPort,
                resultAssembler,
                Clock.systemDefaultZone());
    }

    DashboardService(
            DashboardDataPort dashboardDataPort,
            Clock clock) {
        this(
                dashboardDataPort,
                new DashboardResultAssembler(),
                clock);
    }

    DashboardService(
            DashboardDataPort dashboardDataPort,
            DashboardResultAssembler resultAssembler,
            Clock clock) {
        this.dashboardDataPort = dashboardDataPort;
        this.resultAssembler = resultAssembler;
        this.clock = clock;
    }

    @Override
    public DashboardResult getDashboard(Long userId) {
        return resultAssembler.assemble(
                dashboardDataPort.load(userId),
                LocalDate.now(clock),
                clock.instant());
    }
}
