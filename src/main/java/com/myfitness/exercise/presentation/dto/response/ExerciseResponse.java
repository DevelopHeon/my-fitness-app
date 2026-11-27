package com.myfitness.exercise.presentation.dto.response;

import com.myfitness.exercise.application.port.in.catalog.ExerciseView;
import com.myfitness.exercise.application.port.in.catalog.ExerciseView.Type;

public record ExerciseResponse(
        Long id,
        Type type,
        String name,
        String category
) {
    public static ExerciseResponse from(ExerciseView exercise) {
        return new ExerciseResponse(
                exercise.id(),
                exercise.type(),
                exercise.name(),
                exercise.category().name());
    }
}
