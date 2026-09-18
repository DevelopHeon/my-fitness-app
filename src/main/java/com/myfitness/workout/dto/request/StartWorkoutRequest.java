package com.myfitness.workout.dto.request;

import java.time.LocalDate;

public record StartWorkoutRequest(
        LocalDate workoutDate,
        String memo
) {
}
