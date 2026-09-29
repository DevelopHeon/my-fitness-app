package com.myfitness.routine.application.port.in;

import com.myfitness.routine.application.dto.request.ExerciseSelection;
import com.myfitness.routine.application.dto.response.RoutineResult;
import com.myfitness.routine.application.dto.response.RoutineWorkoutStartResult;
import java.time.LocalDate;
import java.util.List;

public interface RoutineUseCase {
    RoutineResult create(
            Long userId,
            String name,
            List<ExerciseSelection> selections);

    List<RoutineResult> list(Long userId);

    RoutineResult get(Long userId, Long routineId);

    RoutineResult update(
            Long userId,
            Long routineId,
            String name,
            List<ExerciseSelection> selections);

    void delete(Long userId, Long routineId);

    RoutineWorkoutStartResult startWorkout(
            Long userId,
            Long routineId,
            LocalDate workoutDate,
            String memo);
}
