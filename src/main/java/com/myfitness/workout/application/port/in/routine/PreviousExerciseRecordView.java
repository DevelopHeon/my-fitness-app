package com.myfitness.workout.application.port.in.routine;

import java.time.LocalDate;
import java.util.List;

public record PreviousExerciseRecordView(
        Long workoutId,
        LocalDate workoutDate,
        Long workoutExerciseId,
        String exerciseType,
        Long exerciseId,
        String exerciseName,
        List<WorkoutView.SetView> sets
) {}
