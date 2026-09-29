package com.myfitness.routine.application.dto.response;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.routine.domain.model.Routine;
import com.myfitness.routine.domain.model.RoutineExercise;
import java.time.Instant;
import java.util.List;

public record RoutineResult(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        List<ExerciseResult> exercises
) {
    public static RoutineResult from(Routine routine) {
        return new RoutineResult(
                routine.getId(),
                routine.getName(),
                routine.getCreatedAt(),
                routine.getUpdatedAt(),
                routine.getExercises().stream()
                        .map(ExerciseResult::from)
                        .toList());
    }

    public record ExerciseResult(
            Long id,
            ExerciseType exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            int orderIndex
    ) {
        static ExerciseResult from(RoutineExercise exercise) {
            return new ExerciseResult(
                    exercise.getId(),
                    exercise.getExerciseType(),
                    exercise.getExerciseId(),
                    exercise.getExerciseName(),
                    exercise.getCategory().name(),
                    exercise.getOrderIndex());
        }
    }
}
