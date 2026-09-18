package com.myfitness.workout.dto.response;

import com.myfitness.workout.domain.Exercise;
import java.time.Instant;

public record ExerciseResponse(
        Long id,
        String name,
        String category,
        Instant createdAt
) {
    public static ExerciseResponse from(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getCategory(),
                exercise.getCreatedAt());
    }
}
