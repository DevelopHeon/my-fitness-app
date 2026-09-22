package com.myfitness.workout.application.port.in;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.application.command.WorkoutSetCommand;
import com.myfitness.workout.application.result.WorkoutCalendarDayResult;
import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutExercise;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface WorkoutUseCase {
    WorkoutExercise getPreviousExerciseRecord(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId);

    Workout startWorkout(Long userId, LocalDate workoutDate, String memo);

    Workout getWorkout(Long userId, Long workoutId);

    List<Workout> getWorkouts(Long userId, LocalDate from, LocalDate to);

    List<WorkoutCalendarDayResult> getCalendar(Long userId, YearMonth month);

    Workout addExercise(
            Long userId,
            Long workoutId,
            ExerciseType exerciseType,
            Long exerciseId,
            String memo);

    Workout removeExercise(
            Long userId,
            Long workoutId,
            Long workoutExerciseId);

    Workout addSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed);

    Workout addSets(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            List<WorkoutSetCommand> commands);

    Workout updateSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed);

    Workout removeSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId);

    Workout completeWorkout(Long userId, Long workoutId);

    Workout reopenWorkout(Long userId, Long workoutId);
}
