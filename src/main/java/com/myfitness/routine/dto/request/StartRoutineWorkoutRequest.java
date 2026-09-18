package com.myfitness.routine.dto.request;

import java.time.LocalDate;

public record StartRoutineWorkoutRequest(
        LocalDate workoutDate,
        String memo
) {
}
