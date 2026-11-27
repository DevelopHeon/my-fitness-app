package com.myfitness.workout.application.port.in;

import java.time.LocalDate;
import java.util.List;

public record WorkoutCalendarDayView(
        LocalDate date,
        int workoutCount,
        int completedCount,
        List<String> exerciseNames
) {}
