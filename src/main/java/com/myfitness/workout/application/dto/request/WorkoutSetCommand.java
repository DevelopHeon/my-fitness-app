package com.myfitness.workout.application.dto.request;

import java.math.BigDecimal;

public record WorkoutSetCommand(
        BigDecimal weightKg,
        int reps,
        Integer durationSeconds,
        boolean completed
) {}
