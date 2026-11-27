package com.myfitness.workout.application.usecase;

import com.myfitness.workout.application.usecase.response.WorkoutResult;
import java.time.LocalDate;
import java.util.List;

public interface WorkoutInsightUseCase {
    List<WorkoutResult> findCompletedWorkouts(Long userId);

    List<WorkoutResult> findCompletedSince(Long userId, LocalDate from);
}
