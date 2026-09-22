package com.myfitness.exercise.presentation.dto.request;

import com.myfitness.exercise.domain.model.ExerciseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExerciseRequest(
        @NotBlank String name,
        @NotNull ExerciseCategory category
) {
}
