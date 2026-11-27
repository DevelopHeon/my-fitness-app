package com.myfitness.workout.application.usecase;

import com.myfitness.workout.application.usecase.request.WorkoutSetCommand;
import com.myfitness.workout.application.usecase.response.PreviousExerciseRecordResult;
import com.myfitness.workout.domain.model.ExerciseSnapshot;
import com.myfitness.workout.domain.model.ExerciseType;
import com.myfitness.workout.domain.model.Workout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface WorkoutOperationsUseCase {
    Workout start(Long userId, LocalDate workoutDate, String memo);

    Workout startWithExercises(
            Long userId,
            LocalDate workoutDate,
            String memo,
            List<ExerciseSnapshot> exercises);

    Workout getOwned(Long userId, Long workoutId);

    Workout addExercise(
            Workout workout,
            ExerciseSnapshot exercise,
            String memo);

    Workout removeExercise(Workout workout, Long workoutExerciseId);

    Workout addSet(
            Workout workout,
            Long workoutExerciseId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed);

    Workout addSets(
            Workout workout,
            Long workoutExerciseId,
            List<WorkoutSetCommand> commands);

    Workout updateSet(
            Workout workout,
            Long workoutExerciseId,
            Long setId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed);

    Workout removeSet(
            Workout workout,
            Long workoutExerciseId,
            Long setId);

    Workout complete(Workout workout);

    Workout reopen(Workout workout);

    PreviousExerciseRecordResult getPreviousCompletedExercise(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId);
}
