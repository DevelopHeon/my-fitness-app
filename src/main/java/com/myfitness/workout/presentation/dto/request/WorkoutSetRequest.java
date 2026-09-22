package com.myfitness.workout.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record WorkoutSetRequest(
        @DecimalMin("0.0") BigDecimal weightKg,
        @PositiveOrZero int reps,
        @PositiveOrZero Integer durationSeconds,
        boolean completed
) {
}
