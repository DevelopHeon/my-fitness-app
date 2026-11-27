package com.myfitness.workout.presentation.dto.response;

import com.myfitness.workout.application.port.in.routine.PreviousExerciseRecordView;
import java.time.LocalDate;
import java.util.List;

public record PreviousExerciseRecordResponse(
        Long workoutId,
        LocalDate workoutDate,
        Long workoutExerciseId,
        String exerciseType,
        Long exerciseId,
        String exerciseName,
        List<WorkoutResponse.SetEntry> sets
) {
    public static PreviousExerciseRecordResponse from(
            PreviousExerciseRecordView result) {
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
