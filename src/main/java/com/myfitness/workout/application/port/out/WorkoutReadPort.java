package com.myfitness.workout.application.port.out;

import com.myfitness.workout.application.usecase.response.WorkoutCalendarDayResult;
import com.myfitness.workout.application.usecase.response.WorkoutResult;
import java.time.LocalDate;
import java.util.List;

public interface WorkoutReadPort {
    List<WorkoutResult> findByUserIdAndDateRange(Long userId, LocalDate from, LocalDate to);

    List<WorkoutCalendarDayResult> findCalendar(Long userId, LocalDate from, LocalDate to);

    List<WorkoutResult> findCompleted(Long userId);

    List<WorkoutResult> findCompletedSince(Long userId, LocalDate from);
}
