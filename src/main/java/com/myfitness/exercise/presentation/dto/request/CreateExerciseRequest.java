package com.myfitness.exercise.presentation.dto.request;

import com.myfitness.exercise.application.port.in.catalog.ExerciseView.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExerciseRequest(
        @NotBlank String name,
        @NotNull Category category
) {
}
