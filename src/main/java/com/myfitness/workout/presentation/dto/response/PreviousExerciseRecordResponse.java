package com.myfitness.workout.presentation.dto.response;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.application.dto.response.PreviousExerciseRecordResult;
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
    public static PreviousExerciseRecordResponse from(
            PreviousExerciseRecordResult result) {
        return new PreviousExerciseRecordResponse(
                result.workoutId(),
                result.workoutDate(),
                result.workoutExerciseId(),
                result.exerciseType(),
                result.exerciseId(),
                result.exerciseName(),
                result.sets().stream()
                        .map(set -> new WorkoutResponse.SetEntry(
                                set.id(),
                                set.setNumber(),
                                set.weightKg(),
                                set.reps(),
                                set.durationSeconds(),
                                set.completed()))
                        .toList());
    }
}
