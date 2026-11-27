package com.myfitness.workout.application.service;

import com.myfitness.workout.application.usecase.response.WorkoutResult;
import com.myfitness.workout.application.usecase.WorkoutInsightUseCase;
import com.myfitness.workout.application.port.out.WorkoutReadPort;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkoutInsightService implements WorkoutInsightUseCase {
    private final WorkoutReadPort workouts;

    public WorkoutInsightService(WorkoutReadPort workouts) {
        this.workouts = workouts;
    }

    @Override
    public List<WorkoutResult> findCompletedWorkouts(Long userId) {
        return workouts.findCompleted(userId);
    }

    @Override
    public List<WorkoutResult> findCompletedSince(
            Long userId,
            LocalDate from) {
        return workouts.findCompletedSince(userId, from);
    }

}
