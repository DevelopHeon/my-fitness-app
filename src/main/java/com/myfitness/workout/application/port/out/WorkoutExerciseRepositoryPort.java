package com.myfitness.workout.application.port.out;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.domain.model.WorkoutExercise;
import java.util.Optional;

public interface WorkoutExerciseRepositoryPort {
    Optional<WorkoutExercise> findLatestCompletedByExercise(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId);
}
