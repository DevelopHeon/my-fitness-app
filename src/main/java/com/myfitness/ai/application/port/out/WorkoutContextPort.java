package com.myfitness.ai.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface WorkoutContextPort {
    List<WorkoutInsight> findCompletedSince(Long userId, LocalDate from);

    record WorkoutInsight(
            LocalDate workoutDate,
            Instant startedAt,
            List<ExerciseInsight> exercises
    ) {}

    record ExerciseInsight(
            String exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            List<SetInsight> sets
    ) {}

    record SetInsight(
            BigDecimal weightKg,
            int reps,
            boolean completed
    ) {}
}
