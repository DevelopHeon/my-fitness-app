package com.myfitness.dashboard.application.service;

import com.myfitness.dashboard.application.port.out.DashboardDataPort.DashboardSourceData;
import com.myfitness.dashboard.application.result.DashboardResult;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class DashboardResultAssembler {
    private final DashboardWorkoutSummaryBuilder workoutSummaryBuilder =
            new DashboardWorkoutSummaryBuilder();
    private final DashboardBodySummaryBuilder bodySummaryBuilder =
            new DashboardBodySummaryBuilder();
    private final DashboardExerciseSummaryBuilder exerciseSummaryBuilder =
            new DashboardExerciseSummaryBuilder();

    public DashboardResult assemble(
            DashboardSourceData source,
            LocalDate today,
            Instant now) {
        return new DashboardResult(
                today,
                workoutSummaryBuilder.build(
                        source.completedWorkouts(),
                        today),
                bodySummaryBuilder.build(
                        source.bodyRecords(),
                        now.minus(90, ChronoUnit.DAYS)),
                exerciseSummaryBuilder.build(
                        source.completedWorkouts()));
    }
}
