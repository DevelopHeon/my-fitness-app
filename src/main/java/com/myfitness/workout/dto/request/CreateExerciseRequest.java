package com.myfitness.workout.dto.request;

import com.myfitness.workout.domain.ExerciseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExerciseRequest(
        @NotBlank String name,
        @NotNull ExerciseCategory category
) {
}
