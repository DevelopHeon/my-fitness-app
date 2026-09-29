package com.myfitness.routine.presentation.dto.response;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.routine.application.dto.response.RoutineWorkoutStartResult;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.ExerciseView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.PreviousRecordView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.SetView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.WorkoutView;
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
            String status,
            String memo,
            Instant startedAt,
            Instant completedAt,
            List<ExerciseEntry> exercises
    ) {
        static WorkoutEntry from(WorkoutView workout) {
            return new WorkoutEntry(
                    workout.id(),
                    workout.workoutDate(),
                    workout.status(),
                    workout.memo(),
                    workout.startedAt(),
                    workout.completedAt(),
                    workout.exercises().stream()
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
        static ExerciseEntry from(ExerciseView entry) {
            return new ExerciseEntry(
                    entry.id(),
                    entry.exerciseType(),
                    entry.exerciseId(),
                    entry.exerciseName(),
                    entry.category(),
                    entry.orderIndex(),
                    entry.memo(),
                    entry.sets().stream()
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
        static SetEntry from(SetView set) {
            return new SetEntry(
                    set.id(),
                    set.setNumber(),
                    set.weightKg(),
                    set.reps(),
                    set.durationSeconds(),
                    set.completed());
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
        static PreviousRecordEntry from(PreviousRecordView entry) {
            return new PreviousRecordEntry(
                    entry.workoutId(),
                    entry.workoutDate(),
                    entry.workoutExerciseId(),
                    entry.exerciseType(),
                    entry.exerciseId(),
                    entry.exerciseName(),
                    entry.sets().stream()
                            .map(SetEntry::from)
                            .toList());
        }
    }
}
