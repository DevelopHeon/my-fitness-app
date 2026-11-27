package com.myfitness.routine.application.usecase;

import com.myfitness.routine.domain.model.ExerciseSnapshot;
import com.myfitness.routine.domain.model.Routine;
import java.util.List;

public interface RoutineOperationsUseCase {
    Routine create(
            Long userId,
            String name,
            List<ExerciseSnapshot> exercises);

    Routine update(
            Routine routine,
            String name,
            List<ExerciseSnapshot> exercises);

    Routine getOwned(Long userId, Long routineId);

    void delete(Routine routine);
}
