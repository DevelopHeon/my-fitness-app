package com.myfitness.workout.application.dto.response;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutExercise;
import com.myfitness.workout.domain.model.WorkoutSet;
import com.myfitness.workout.domain.model.WorkoutStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WorkoutResult(
        Long id,
        LocalDate workoutDate,
        WorkoutStatus status,
        String memo,
        Instant startedAt,
        Instant completedAt,
        List<ExerciseResult> exercises
) {
    public static WorkoutResult from(Workout workout) {
        return new WorkoutResult(
                workout.getId(),
                workout.getWorkoutDate(),
                workout.getStatus(),
                workout.getMemo(),
                workout.getStartedAt(),
                workout.getCompletedAt(),
                workout.getExercises().stream()
                        .map(ExerciseResult::from)
                        .toList());
    }

    public record ExerciseResult(
            Long id,
            ExerciseType exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            int orderIndex,
            String memo,
            List<SetResult> sets
    ) {
        static ExerciseResult from(WorkoutExercise exercise) {
            return new ExerciseResult(
                    exercise.getId(),
                    exercise.getExerciseType(),
                    exercise.getExerciseId(),
                    exercise.getExerciseName(),
                    exercise.getCategory().name(),
                    exercise.getOrderIndex(),
                    exercise.getMemo(),
                    exercise.getSets().stream()
                            .map(SetResult::from)
                            .toList());
        }
    }

    public record SetResult(
            Long id,
            int setNumber,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed
    ) {
        static SetResult from(WorkoutSet set) {
            return new SetResult(
                    set.getId(),
                    set.getSetNumber(),
                    set.getWeightKg(),
                    set.getReps(),
                    set.getDurationSeconds(),
                    set.isCompleted());
        }
    }
}
