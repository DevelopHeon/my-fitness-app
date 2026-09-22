package com.myfitness.dashboard.application.port;

import com.myfitness.body.domain.model.BodyRecord;
import com.myfitness.workout.domain.model.Workout;
import java.util.List;

public interface DashboardQueryPort {
    DashboardSourceData load(Long userId);

    record DashboardSourceData(
            List<Workout> completedWorkouts,
            List<BodyRecord> bodyRecords
    ) {}
}
