package com.myfitness.workout.domain.repository;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.domain.model.WorkoutExercise;
import java.util.Optional;

public interface WorkoutExerciseRepository {
    Optional<WorkoutExercise> findLatestCompletedByExercise(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId);
}
