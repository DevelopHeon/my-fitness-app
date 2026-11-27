package com.myfitness.workout.infrastructure.module;

import com.myfitness.exercise.application.port.in.catalog.ExerciseCatalogQuery;
import com.myfitness.exercise.application.port.in.catalog.ExerciseView;
import com.myfitness.exercise.application.port.in.catalog.ExerciseView.Type;
import com.myfitness.workout.application.port.out.ExerciseCatalogPort;
import com.myfitness.workout.domain.model.ExerciseCategory;
import com.myfitness.workout.domain.model.ExerciseSnapshot;
import com.myfitness.workout.domain.model.ExerciseType;
import org.springframework.stereotype.Component;

@Component
public class WorkoutExerciseCatalogAdapter implements ExerciseCatalogPort {
    private final ExerciseCatalogQuery catalog;

    public WorkoutExerciseCatalogAdapter(ExerciseCatalogQuery catalog) {
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
