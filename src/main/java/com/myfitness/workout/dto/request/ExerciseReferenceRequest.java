package com.myfitness.workout.dto.request;

import com.myfitness.workout.domain.ExerciseType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExerciseReferenceRequest(
        @NotNull ExerciseType exerciseType,
        @NotNull @Positive Long exerciseId
) {
}
