package com.myfitness.workout.application.service;

import com.myfitness.workout.application.dto.response.WorkoutResult;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery;
import com.myfitness.workout.application.port.out.WorkoutRepositoryPort;
import com.myfitness.workout.domain.model.WorkoutStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkoutInsightService implements WorkoutInsightQuery {
    private final WorkoutRepositoryPort workoutRepositoryPort;

    public WorkoutInsightService(WorkoutRepositoryPort workoutRepositoryPort) {
        this.workoutRepositoryPort = workoutRepositoryPort;
    }

    @Override
    public List<WorkoutInsight> findCompletedWorkouts(Long userId) {
        return workoutRepositoryPort.findByUserIdAndStatus(userId, WorkoutStatus.COMPLETED).stream()
                .map(WorkoutResult::from)
                .map(WorkoutInsight::from)
                .toList();
    }

    @Override
    public List<WorkoutInsight> findCompletedSince(
            Long userId,
            java.time.LocalDate from) {
        return workoutRepositoryPort.findByUserIdAndStatusSince(userId, WorkoutStatus.COMPLETED, from).stream()
                .map(WorkoutResult::from)
                .map(WorkoutInsight::from)
                .toList();
    }

}
