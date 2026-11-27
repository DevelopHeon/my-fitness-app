package com.myfitness.exercise.application.usecase;

import java.util.List;

public interface ExerciseCatalogUseCase {
    List<ExerciseValue> list(Long userId);

    ExerciseValue getAvailable(
            Long userId,
            String type,
            Long exerciseId);
}
