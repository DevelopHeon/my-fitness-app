package com.myfitness.workout.application.usecase.response;

import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutExercise;
import com.myfitness.workout.domain.model.WorkoutStatus;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record WorkoutCalendarDayResult(
        LocalDate date,
        int workoutCount,
        int completedCount,
        List<String> exerciseNames
) {
    public static List<WorkoutCalendarDayResult> groupByDate(List<Workout> workouts) {
        Map<LocalDate, List<Workout>> byDate = workouts.stream()
                .collect(Collectors.groupingBy(Workout::getWorkoutDate, TreeMap::new, Collectors.toList()));
        return byDate.entrySet().stream()
                .map(entry -> from(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static WorkoutCalendarDayResult from(LocalDate date, List<Workout> workouts) {
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
