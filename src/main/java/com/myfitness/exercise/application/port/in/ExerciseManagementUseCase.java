package com.myfitness.exercise.application.port.in;

import com.myfitness.exercise.domain.model.ExerciseCategory;
import com.myfitness.exercise.domain.model.ExerciseReference;

public interface ExerciseManagementUseCase {
    ExerciseReference createCustom(
            Long userId,
            String name,
            ExerciseCategory category);

    ExerciseReference updateCustom(
            Long userId,
            Long exerciseId,
            String name,
            ExerciseCategory category);

    void deleteCustom(Long userId, Long exerciseId);
}
