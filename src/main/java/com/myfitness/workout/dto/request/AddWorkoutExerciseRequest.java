package com.myfitness.workout.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddWorkoutExerciseRequest(
        @NotNull @Positive Long exerciseId,
        String memo
) {
}
