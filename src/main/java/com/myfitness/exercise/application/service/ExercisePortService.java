package com.myfitness.exercise.application.service;

import com.myfitness.exercise.application.port.in.ExerciseManagementPort;
import com.myfitness.exercise.application.port.in.catalog.ExerciseCatalogQuery;
import com.myfitness.exercise.application.port.in.catalog.ExerciseView;
import com.myfitness.exercise.application.port.in.catalog.ExerciseView.Category;
import com.myfitness.exercise.application.port.in.catalog.ExerciseView.Type;
import com.myfitness.exercise.application.usecase.ExerciseManagementUseCase;
import com.myfitness.exercise.application.usecase.ExerciseCatalogUseCase;
import com.myfitness.exercise.application.usecase.ExerciseValue;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExercisePortService implements ExerciseManagementPort, ExerciseCatalogQuery {
    private final ExerciseManagementUseCase management;
    private final ExerciseCatalogUseCase catalog;

    public ExercisePortService(ExerciseManagementUseCase management, ExerciseCatalogUseCase catalog) {
        this.management = management;
        this.catalog = catalog;
    }

    @Override
    public ExerciseView createCustom(Long userId, String name, Category category) {
        return toView(management.createCustom(userId, name, category.name()));
    }

    @Override
    public ExerciseView updateCustom(Long userId, Long exerciseId, String name, Category category) {
        return toView(management.updateCustom(userId, exerciseId, name, category.name()));
    }

    @Override
    public void deleteCustom(Long userId, Long exerciseId) {
        management.deleteCustom(userId, exerciseId);
    }

    @Override
    public List<ExerciseView> list(Long userId) {
        return catalog.list(userId).stream().map(ExercisePortService::toView).toList();
    }

    @Override
    public ExerciseView getAvailable(Long userId, Type type, Long exerciseId) {
        return toView(catalog.getAvailable(userId, type == null ? null : type.name(), exerciseId));
    }

    private static ExerciseView toView(ExerciseValue value) {
        return new ExerciseView(
                value.id(), Type.valueOf(value.type()), value.name(), Category.valueOf(value.category()),
                value.ownerUserId());
    }
}
