package com.myfitness.routine.presentation.dto.response;

import com.myfitness.routine.application.result.RoutineWorkoutStartResult;
import com.myfitness.workout.presentation.dto.response.PreviousExerciseRecordResponse;
import com.myfitness.workout.presentation.dto.response.WorkoutResponse;
import java.util.List;

public record RoutineWorkoutStartResponse(
        WorkoutResponse workout,
        List<PreviousExerciseRecordResponse> previousRecords
) {
    public static RoutineWorkoutStartResponse from(
            RoutineWorkoutStartResult result) {
        return new RoutineWorkoutStartResponse(
                WorkoutResponse.from(result.workout()),
                result.previousRecords().stream()
                        .map(PreviousExerciseRecordResponse::from)
                        .toList());
    }
}
