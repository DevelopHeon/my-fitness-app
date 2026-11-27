package com.myfitness.workout.application.port.out;

import com.myfitness.workout.domain.model.ExerciseSnapshot;
import com.myfitness.workout.domain.model.ExerciseType;

public interface ExerciseCatalogPort {
    ExerciseSnapshot getAvailable(Long userId, ExerciseType type, Long exerciseId);
}
