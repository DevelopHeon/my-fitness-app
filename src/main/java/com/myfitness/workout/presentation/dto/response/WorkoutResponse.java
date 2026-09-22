package com.myfitness.workout.presentation.dto.response;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.application.result.WorkoutResult;
import com.myfitness.workout.domain.model.WorkoutStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WorkoutResponse(
        Long id,
        LocalDate workoutDate,
        WorkoutStatus status,
        String memo,
        Instant startedAt,
        Instant completedAt,
        List<ExerciseEntry> exercises
) {
    public static WorkoutResponse from(WorkoutResult result) {
        return new WorkoutResponse(
                result.id(),
                result.workoutDate(),
                result.status(),
                result.memo(),
                result.startedAt(),
                result.completedAt(),
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
            int orderIndex,
            String memo,
            List<SetEntry> sets
    ) {
        static ExerciseEntry from(WorkoutResult.ExerciseResult exercise) {
            return new ExerciseEntry(
                    exercise.id(),
                    exercise.exerciseType(),
                    exercise.exerciseId(),
                    exercise.exerciseName(),
                    exercise.category(),
                    exercise.orderIndex(),
                    exercise.memo(),
                    exercise.sets().stream()
                            .map(SetEntry::from)
                            .toList());
        }
    }

    public record SetEntry(
            Long id,
            int setNumber,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed
    ) {
        static SetEntry from(WorkoutResult.SetResult set) {
            return new SetEntry(
                    set.id(),
                    set.setNumber(),
                    set.weightKg(),
                    set.reps(),
                    set.durationSeconds(),
                    set.completed());
        }
    }
}
