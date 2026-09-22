package com.myfitness.workout.application.port.in.routine;

import com.myfitness.exercise.domain.model.ExerciseReference;
import com.myfitness.exercise.domain.model.ExerciseType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface WorkoutRoutineUseCase {
    RoutineWorkoutView startWorkout(
            Long userId,
            LocalDate workoutDate,
            String memo,
            List<ExerciseReference> exercises);

    record RoutineWorkoutView(
            WorkoutView workout,
            List<PreviousRecordView> previousRecords
    ) {}

    record WorkoutView(
            Long id,
            LocalDate workoutDate,
            String status,
            String memo,
            Instant startedAt,
            Instant completedAt,
            List<ExerciseView> exercises
    ) {}

    record ExerciseView(
            Long id,
            ExerciseType exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            int orderIndex,
            String memo,
            List<SetView> sets
    ) {}

    record SetView(
            Long id,
            int setNumber,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed
    ) {}

    record PreviousRecordView(
            Long workoutId,
            LocalDate workoutDate,
            Long workoutExerciseId,
            ExerciseType exerciseType,
            Long exerciseId,
            String exerciseName,
            List<SetView> sets
    ) {}
}
