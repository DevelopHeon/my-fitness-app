package com.myfitness.routine.presentation.dto.response;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.routine.application.result.RoutineWorkoutStartResult;
import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutExercise;
import com.myfitness.workout.domain.model.WorkoutSet;
import com.myfitness.workout.domain.model.WorkoutStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RoutineWorkoutStartResponse(
        WorkoutEntry workout,
        List<PreviousRecordEntry> previousRecords
) {
    public static RoutineWorkoutStartResponse from(
            RoutineWorkoutStartResult result) {
        return new RoutineWorkoutStartResponse(
                WorkoutEntry.from(result.workout()),
                result.previousRecords().stream()
                        .map(PreviousRecordEntry::from)
                        .toList());
    }

    public record WorkoutEntry(
            Long id,
            LocalDate workoutDate,
            WorkoutStatus status,
            String memo,
            Instant startedAt,
            Instant completedAt,
            List<ExerciseEntry> exercises
    ) {
        static WorkoutEntry from(Workout workout) {
            return new WorkoutEntry(
                    workout.getId(),
                    workout.getWorkoutDate(),
                    workout.getStatus(),
                    workout.getMemo(),
                    workout.getStartedAt(),
                    workout.getCompletedAt(),
                    workout.getExercises().stream()
                            .map(ExerciseEntry::from)
                            .toList());
        }
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
        static ExerciseEntry from(WorkoutExercise entry) {
            return new ExerciseEntry(
                    entry.getId(),
                    entry.getExerciseType(),
                    entry.getExerciseId(),
                    entry.getExerciseName(),
                    entry.getCategory().name(),
                    entry.getOrderIndex(),
                    entry.getMemo(),
                    entry.getSets().stream()
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
        static SetEntry from(WorkoutSet set) {
            return new SetEntry(
                    set.getId(),
                    set.getSetNumber(),
                    set.getWeightKg(),
                    set.getReps(),
                    set.getDurationSeconds(),
                    set.isCompleted());
        }
    }

    public record PreviousRecordEntry(
            Long workoutId,
            LocalDate workoutDate,
            Long workoutExerciseId,
            ExerciseType exerciseType,
            Long exerciseId,
            String exerciseName,
            List<SetEntry> sets
    ) {
        static PreviousRecordEntry from(WorkoutExercise entry) {
            return new PreviousRecordEntry(
                    entry.getWorkout().getId(),
                    entry.getWorkout().getWorkoutDate(),
                    entry.getId(),
                    entry.getExerciseType(),
                    entry.getExerciseId(),
                    entry.getExerciseName(),
                    entry.getSets().stream()
                            .map(SetEntry::from)
                            .toList());
        }
    }
}
