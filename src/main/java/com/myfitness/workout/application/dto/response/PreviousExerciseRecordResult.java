package com.myfitness.workout.application.dto.response;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.domain.model.WorkoutExercise;
import java.time.LocalDate;
import java.util.List;

public record PreviousExerciseRecordResult(
        Long workoutId,
        LocalDate workoutDate,
        Long workoutExerciseId,
        ExerciseType exerciseType,
        Long exerciseId,
        String exerciseName,
        List<WorkoutResult.SetResult> sets
) {
    public static PreviousExerciseRecordResult from(
            WorkoutExercise exercise) {
        if (exercise == null) {
            return null;
        }
        return new PreviousExerciseRecordResult(
                exercise.getWorkout().getId(),
                exercise.getWorkout().getWorkoutDate(),
                exercise.getId(),
                exercise.getExerciseType(),
                exercise.getExerciseId(),
                exercise.getExerciseName(),
                exercise.getSets().stream()
                        .map(set -> new WorkoutResult.SetResult(
                                set.getId(),
                                set.getSetNumber(),
                                set.getWeightKg(),
                                set.getReps(),
                                set.getDurationSeconds(),
                                set.isCompleted()))
                        .toList());
    }
}
