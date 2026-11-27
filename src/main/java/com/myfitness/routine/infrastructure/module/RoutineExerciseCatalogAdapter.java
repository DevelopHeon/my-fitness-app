package com.myfitness.routine.infrastructure.module;

import com.myfitness.exercise.application.port.in.catalog.ExerciseCatalogQuery;
import com.myfitness.exercise.application.port.in.catalog.ExerciseView;
import com.myfitness.exercise.application.port.in.catalog.ExerciseView.Type;
import com.myfitness.routine.application.port.out.ExerciseCatalogPort;
import com.myfitness.routine.domain.model.ExerciseCategory;
import com.myfitness.routine.domain.model.ExerciseSnapshot;
import com.myfitness.routine.domain.model.ExerciseType;
import org.springframework.stereotype.Component;

@Component
public class RoutineExerciseCatalogAdapter implements ExerciseCatalogPort {
    private final ExerciseCatalogQuery catalog;

    public RoutineExerciseCatalogAdapter(ExerciseCatalogQuery catalog) {
        this.catalog = catalog;
    }

    @Override
    public ExerciseSnapshot getAvailable(Long userId, ExerciseType type, Long exerciseId) {
        ExerciseView exercise = catalog.getAvailable(
                userId, type == null ? null : Type.valueOf(type.name()), exerciseId);
        return new ExerciseSnapshot(
                ExerciseType.valueOf(exercise.type().name()), exercise.id(), exercise.name(),
                ExerciseCategory.valueOf(exercise.category().name()), exercise.ownerUserId());
    }
}
