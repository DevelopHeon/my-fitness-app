package com.myfitness.exercise.application.port.in.catalog;

import com.myfitness.exercise.domain.model.ExerciseReference;
import com.myfitness.exercise.domain.model.ExerciseType;
import java.util.List;

public interface ExerciseCatalogQuery {
    List<ExerciseReference> list(Long userId);

    ExerciseReference getAvailable(
            Long userId,
            ExerciseType type,
            Long exerciseId);
}
