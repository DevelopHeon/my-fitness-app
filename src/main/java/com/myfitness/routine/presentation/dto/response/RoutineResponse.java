package com.myfitness.routine.presentation.dto.response;

import com.myfitness.routine.domain.model.Routine;
import com.myfitness.routine.domain.model.RoutineExercise;
import com.myfitness.exercise.domain.model.ExerciseType;
import java.time.Instant;
import java.util.List;

public record RoutineResponse(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        List<ExerciseEntry> exercises
) {
    public static RoutineResponse from(Routine routine) {
        return new RoutineResponse(
                routine.getId(),
                routine.getName(),
                routine.getCreatedAt(),
                routine.getUpdatedAt(),
                routine.getExercises().stream().map(ExerciseEntry::from).toList());
    }

    public record ExerciseEntry(
            Long id,
            ExerciseType exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            int orderIndex
    ) {
        static ExerciseEntry from(RoutineExercise entry) {
            return new ExerciseEntry(
                    entry.getId(),
                    entry.getExerciseType(),
                    entry.getExerciseId(),
                    entry.getExerciseName(),
                    entry.getCategory().name(),
                    entry.getOrderIndex());
        }
    }
}
