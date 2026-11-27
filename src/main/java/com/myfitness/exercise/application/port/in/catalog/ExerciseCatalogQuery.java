package com.myfitness.exercise.application.port.in.catalog;

import com.myfitness.exercise.application.port.in.catalog.ExerciseView.Type;
import java.util.List;

public interface ExerciseCatalogQuery {
    List<ExerciseView> list(Long userId);

    ExerciseView getAvailable(
            Long userId,
            Type type,
            Long exerciseId);
}
