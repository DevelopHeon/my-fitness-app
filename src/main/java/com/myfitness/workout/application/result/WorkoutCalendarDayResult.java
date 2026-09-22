package com.myfitness.workout.application.result;

import java.time.LocalDate;
import java.util.List;

public record WorkoutCalendarDayResult(
        LocalDate date,
        int workoutCount,
        int completedCount,
        List<String> exerciseNames
) {}
