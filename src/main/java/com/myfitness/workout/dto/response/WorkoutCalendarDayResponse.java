package com.myfitness.workout.dto.response;

import java.time.LocalDate;
import java.util.List;

public record WorkoutCalendarDayResponse(
        LocalDate date,
        int workoutCount,
        int completedCount,
        List<String> exerciseNames
) {
}
