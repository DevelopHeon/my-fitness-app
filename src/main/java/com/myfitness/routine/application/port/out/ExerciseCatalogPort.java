package com.myfitness.routine.application.port.out;

import com.myfitness.routine.domain.model.ExerciseSnapshot;
import com.myfitness.routine.domain.model.ExerciseType;

public interface ExerciseCatalogPort {
    ExerciseSnapshot getAvailable(Long userId, ExerciseType type, Long exerciseId);
}
