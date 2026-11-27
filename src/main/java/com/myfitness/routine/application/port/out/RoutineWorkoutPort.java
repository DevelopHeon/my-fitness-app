package com.myfitness.routine.application.port.out;

import com.myfitness.routine.application.usecase.response.RoutineWorkoutStartResult;
import com.myfitness.routine.domain.model.ExerciseSnapshot;
import java.time.LocalDate;
import java.util.List;

public interface RoutineWorkoutPort {
    RoutineWorkoutStartResult startWorkout(
            Long userId, LocalDate workoutDate, String memo, List<ExerciseSnapshot> exercises);
}
