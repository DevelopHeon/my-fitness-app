package com.myfitness.routine.presentation.dto.request;

import com.myfitness.exercise.domain.model.ExerciseType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExerciseReferenceRequest(
        @NotNull ExerciseType exerciseType,
        @NotNull @Positive Long exerciseId
) {
}
