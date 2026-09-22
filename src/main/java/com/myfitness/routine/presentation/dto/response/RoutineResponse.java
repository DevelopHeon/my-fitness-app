package com.myfitness.routine.presentation.dto.response;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.routine.application.result.RoutineResult;
import java.time.Instant;
import java.util.List;

public record RoutineResponse(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        List<ExerciseEntry> exercises
) {
    public static RoutineResponse from(RoutineResult result) {
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
            ExerciseType exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            int orderIndex
    ) {
        static ExerciseEntry from(RoutineResult.ExerciseResult exercise) {
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
