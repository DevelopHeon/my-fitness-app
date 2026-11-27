package com.myfitness.workout.application.port.in.routine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WorkoutView(
        Long id,
        LocalDate workoutDate,
        String status,
        String memo,
        Instant startedAt,
        Instant completedAt,
        List<ExerciseView> exercises
) {

    public record ExerciseView(
            Long id,
            String exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            int orderIndex,
            String memo,
            List<SetView> sets
    ) {}

    public record SetView(
            Long id,
            int setNumber,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed
    ) {}
}
