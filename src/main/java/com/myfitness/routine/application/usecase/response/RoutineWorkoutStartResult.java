package com.myfitness.routine.application.usecase.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RoutineWorkoutStartResult(Workout workout, List<PreviousRecord> previousRecords) {
    public record Workout(
            Long id, LocalDate workoutDate, String status, String memo, Instant startedAt, Instant completedAt,
            List<Exercise> exercises) {}

    public record Exercise(
            Long id, String exerciseType, Long exerciseId, String exerciseName, String category, int orderIndex,
            String memo, List<Set> sets) {}

    public record Set(
            Long id, int setNumber, BigDecimal weightKg, int reps, Integer durationSeconds, boolean completed) {}

    public record PreviousRecord(
            Long workoutId, LocalDate workoutDate, Long workoutExerciseId, String exerciseType, Long exerciseId,
            String exerciseName, List<Set> sets) {}
}
