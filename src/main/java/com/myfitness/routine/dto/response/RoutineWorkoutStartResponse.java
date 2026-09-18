package com.myfitness.routine.dto.response;

import com.myfitness.workout.dto.response.PreviousExerciseRecordResponse;
import com.myfitness.workout.dto.response.WorkoutResponse;
import java.util.List;

public record RoutineWorkoutStartResponse(
        WorkoutResponse workout,
        List<PreviousExerciseRecordResponse> previousRecords
) {
}
