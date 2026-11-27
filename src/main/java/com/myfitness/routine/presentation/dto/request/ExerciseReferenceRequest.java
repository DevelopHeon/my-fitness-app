package com.myfitness.routine.presentation.dto.request;

import com.myfitness.routine.application.port.in.RoutinePort.ExerciseType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExerciseReferenceRequest(
        @NotNull ExerciseType exerciseType,
        @NotNull @Positive Long exerciseId
) {
}
