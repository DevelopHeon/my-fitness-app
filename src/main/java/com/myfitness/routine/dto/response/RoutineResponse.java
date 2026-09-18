package com.myfitness.routine.dto.response;

import com.myfitness.routine.domain.Routine;
import com.myfitness.routine.domain.RoutineExercise;
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
            Long exerciseId,
            String exerciseName,
            String category,
            int orderIndex
    ) {
        static ExerciseEntry from(RoutineExercise entry) {
            return new ExerciseEntry(
                    entry.getId(),
                    entry.getExercise().getId(),
                    entry.getExercise().getName(),
                    entry.getExercise().getCategory(),
                    entry.getOrderIndex());
        }
    }
}
