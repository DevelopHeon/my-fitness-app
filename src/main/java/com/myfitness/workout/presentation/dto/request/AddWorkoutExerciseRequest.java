package com.myfitness.workout.presentation.dto.request;

import com.myfitness.exercise.domain.model.ExerciseType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddWorkoutExerciseRequest(
        @NotNull ExerciseType exerciseType,
        @NotNull @Positive Long exerciseId,
        String memo
) {
}
