package com.myfitness.workout.application.port.in;

import com.myfitness.workout.application.port.in.routine.PreviousExerciseRecordView;
import com.myfitness.workout.application.port.in.routine.WorkoutView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface WorkoutPort {
    PreviousExerciseRecordView getPreviousExerciseRecord(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId);

    WorkoutView startWorkout(Long userId, LocalDate workoutDate, String memo);

    WorkoutView getWorkout(Long userId, Long workoutId);

    List<WorkoutView> getWorkouts(Long userId, LocalDate from, LocalDate to);

    List<WorkoutCalendarDayView> getCalendar(Long userId, YearMonth month);

    WorkoutView addExercise(
            Long userId,
            Long workoutId,
            ExerciseType exerciseType,
            Long exerciseId,
            String memo);

    WorkoutView removeExercise(
            Long userId,
            Long workoutId,
            Long workoutExerciseId);

    WorkoutView addSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed);

    WorkoutView addSets(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            List<WorkoutSetInput> commands);

    WorkoutView updateSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed);

    WorkoutView removeSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId);

    WorkoutView completeWorkout(Long userId, Long workoutId);

    WorkoutView reopenWorkout(Long userId, Long workoutId);

    enum ExerciseType {
        DEFAULT, CUSTOM
    }

    record WorkoutSetInput(BigDecimal weightKg, int reps, Integer durationSeconds, boolean completed) {}
}
