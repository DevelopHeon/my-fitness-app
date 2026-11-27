package com.myfitness.workout.presentation.dto.response;

import com.myfitness.workout.application.port.in.WorkoutCalendarDayView;
import java.time.LocalDate;
import java.util.List;

public record WorkoutCalendarDayResponse(
        LocalDate date,
        int workoutCount,
        int completedCount,
        List<String> exerciseNames
) {
    public static WorkoutCalendarDayResponse from(
            WorkoutCalendarDayView result) {
        return new WorkoutCalendarDayResponse(
                result.date(),
                result.workoutCount(),
                result.completedCount(),
                result.exerciseNames());
    }
}
