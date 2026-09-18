package com.myfitness.workout.dto.response;

import com.myfitness.workout.domain.ExerciseType;
import com.myfitness.workout.domain.WorkoutExercise;
import java.time.LocalDate;
import java.util.List;

public record PreviousExerciseRecordResponse(
        Long workoutId,
        LocalDate workoutDate,
        Long workoutExerciseId,
        ExerciseType exerciseType,
        Long exerciseId,
        String exerciseName,
        List<WorkoutResponse.SetEntry> sets
) {
    public static PreviousExerciseRecordResponse from(WorkoutExercise entry) {
        return new PreviousExerciseRecordResponse(
                entry.getWorkout().getId(),
                entry.getWorkout().getWorkoutDate(),
                entry.getId(),
                entry.getExerciseType(),
                entry.getExerciseId(),
                entry.getExerciseName(),
                entry.getSets().stream().map(set -> new WorkoutResponse.SetEntry(
                        set.getId(),
                        set.getSetNumber(),
                        set.getWeightKg(),
                        set.getReps(),
                        set.getDurationSeconds(),
                        set.isCompleted())).toList());
    }
}
