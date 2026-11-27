package com.myfitness.routine.presentation.dto.response;

import com.myfitness.routine.application.port.in.RoutineView;
import java.time.Instant;
import java.util.List;

public record RoutineResponse(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        List<ExerciseEntry> exercises
) {
    public static RoutineResponse from(RoutineView result) {
        return new RoutineResponse(
                result.id(),
                result.name(),
                result.createdAt(),
                result.updatedAt(),
                result.exercises().stream()
                        .map(ExerciseEntry::from)
                        .toList());
    }

    public record ExerciseEntry(
            Long id,
            String exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            int orderIndex
    ) {
        static ExerciseEntry from(RoutineView.ExerciseView exercise) {
            return new ExerciseEntry(
                    exercise.id(),
                    exercise.exerciseType(),
                    exercise.exerciseId(),
                    exercise.exerciseName(),
                    exercise.category(),
                    exercise.orderIndex());
        }
    }
}
