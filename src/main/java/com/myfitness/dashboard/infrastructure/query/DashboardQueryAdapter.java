package com.myfitness.dashboard.infrastructure.query;

import com.myfitness.body.domain.repository.BodyRecordRepository;
import com.myfitness.dashboard.application.port.DashboardQueryPort;
import com.myfitness.workout.domain.model.WorkoutStatus;
import com.myfitness.workout.domain.repository.WorkoutRepository;
import org.springframework.stereotype.Component;

@Component
public class DashboardQueryAdapter implements DashboardQueryPort {
    private final WorkoutRepository workoutRepository;
    private final BodyRecordRepository bodyRecordRepository;

    public DashboardQueryAdapter(
            WorkoutRepository workoutRepository,
            BodyRecordRepository bodyRecordRepository) {
        this.workoutRepository = workoutRepository;
        this.bodyRecordRepository = bodyRecordRepository;
    }

    @Override
    public DashboardSourceData load(Long userId) {
        return new DashboardSourceData(
                workoutRepository.findByUserIdAndStatus(
                        userId, WorkoutStatus.COMPLETED),
                bodyRecordRepository.findAllByUserId(userId));
    }
}
