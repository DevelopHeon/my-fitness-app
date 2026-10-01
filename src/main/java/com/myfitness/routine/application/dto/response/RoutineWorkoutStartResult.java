package com.myfitness.routine.application.dto.response;

import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.PreviousRecordView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.RoutineWorkoutView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.WorkoutView;
import java.util.List;

public record RoutineWorkoutStartResult(
        WorkoutView workout,
        List<PreviousRecordView> previousRecords
) {
    public static RoutineWorkoutStartResult from(RoutineWorkoutView result) {
        return new RoutineWorkoutStartResult(result.workout(), result.previousRecords());
    }
}
