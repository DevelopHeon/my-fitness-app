package com.myfitness.routine.presentation.dto.request;

import java.time.LocalDate;

public record StartRoutineWorkoutRequest(
        LocalDate workoutDate,
        String memo
) {
}
