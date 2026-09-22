package com.myfitness.workout.application.command;

import java.math.BigDecimal;

public record WorkoutSetCommand(
        BigDecimal weightKg,
        int reps,
        Integer durationSeconds,
        boolean completed
) {}
