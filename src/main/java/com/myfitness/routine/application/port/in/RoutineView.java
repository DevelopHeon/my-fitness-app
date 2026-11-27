package com.myfitness.routine.application.port.in;

import java.time.Instant;
import java.util.List;

public record RoutineView(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        List<ExerciseView> exercises
) {

    public record ExerciseView(
            Long id,
            String exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            int orderIndex
    ) {}
}
