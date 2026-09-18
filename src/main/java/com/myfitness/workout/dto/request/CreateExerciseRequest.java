package com.myfitness.workout.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateExerciseRequest(
        @NotBlank String name,
        String category
) {
}
