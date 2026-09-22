package com.myfitness.exercise.presentation.dto.response;

import com.myfitness.exercise.domain.model.ExerciseReference;
import com.myfitness.exercise.domain.model.ExerciseType;

public record ExerciseResponse(
        Long id,
        ExerciseType type,
        String name,
        String category
) {
    public static ExerciseResponse from(ExerciseReference exercise) {
        return new ExerciseResponse(
                exercise.id(),
                exercise.type(),
                exercise.name(),
                exercise.category().name());
    }
}
