package com.myfitness.workout.dto.response;

import com.myfitness.workout.domain.ExerciseReference;
import com.myfitness.workout.domain.ExerciseType;

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
