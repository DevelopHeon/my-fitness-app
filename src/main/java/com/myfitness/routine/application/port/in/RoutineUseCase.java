package com.myfitness.routine.application.port.in;

import com.myfitness.routine.application.command.ExerciseSelection;
import com.myfitness.routine.application.result.RoutineWorkoutStartResult;
import com.myfitness.routine.domain.model.Routine;
import java.time.LocalDate;
import java.util.List;

public interface RoutineUseCase {
    Routine create(
            Long userId,
            String name,
            List<ExerciseSelection> selections);

    List<Routine> list(Long userId);

    Routine get(Long userId, Long routineId);

    Routine update(
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
