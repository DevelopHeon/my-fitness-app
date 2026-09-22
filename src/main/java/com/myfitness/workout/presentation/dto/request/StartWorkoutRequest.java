package com.myfitness.workout.presentation.dto.request;

import java.time.LocalDate;

public record StartWorkoutRequest(
        LocalDate workoutDate,
        String memo
) {
}
