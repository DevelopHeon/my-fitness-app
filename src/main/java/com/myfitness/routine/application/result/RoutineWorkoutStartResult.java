package com.myfitness.routine.application.result;

import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutExercise;
import java.util.List;

public record RoutineWorkoutStartResult(
        Workout workout,
        List<WorkoutExercise> previousRecords
) {}
