package com.myfitness.workout.application.usecase;

import com.myfitness.workout.application.usecase.request.WorkoutSetCommand;
import com.myfitness.workout.application.usecase.response.PreviousExerciseRecordResult;
import com.myfitness.workout.application.usecase.response.WorkoutCalendarDayResult;
import com.myfitness.workout.application.usecase.response.WorkoutResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface WorkoutUseCase {
    PreviousExerciseRecordResult getPreviousExerciseRecord(
            Long userId,
            String exerciseType,
            Long exerciseId);

    WorkoutResult startWorkout(Long userId, LocalDate workoutDate, String memo);

    WorkoutResult getWorkout(Long userId, Long workoutId);

    List<WorkoutResult> getWorkouts(Long userId, LocalDate from, LocalDate to);

    List<WorkoutCalendarDayResult> getCalendar(Long userId, YearMonth month);

    WorkoutResult addExercise(
            Long userId,
            Long workoutId,
            String exerciseType,
            Long exerciseId,
            String memo);

    WorkoutResult removeExercise(
            Long userId,
            Long workoutId,
            Long workoutExerciseId);

    WorkoutResult addSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed);

    WorkoutResult addSets(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            List<WorkoutSetCommand> commands);

    WorkoutResult updateSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed);

    WorkoutResult removeSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId);

    WorkoutResult completeWorkout(Long userId, Long workoutId);

    WorkoutResult reopenWorkout(Long userId, Long workoutId);
}
