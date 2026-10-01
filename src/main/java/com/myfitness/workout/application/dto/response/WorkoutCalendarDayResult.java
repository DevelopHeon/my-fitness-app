package com.myfitness.workout.application.dto.response;

import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutExercise;
import com.myfitness.workout.domain.model.WorkoutStatus;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

public record WorkoutCalendarDayResult(
        LocalDate date,
        int workoutCount,
        int completedCount,
        List<String> exerciseNames
) {
    public static WorkoutCalendarDayResult from(LocalDate date, List<Workout> workouts) {
        LinkedHashSet<String> exerciseNames = new LinkedHashSet<>();
        int completedCount = 0;
        for (Workout workout : workouts) {
            if (workout.getStatus() == WorkoutStatus.COMPLETED) {
                completedCount++;
            }
            workout.getExercises().stream()
                    .map(WorkoutExercise::getExerciseName)
                    .forEach(exerciseNames::add);
        }
        return new WorkoutCalendarDayResult(
                date, workouts.size(), completedCount, List.copyOf(exerciseNames));
    }
}
