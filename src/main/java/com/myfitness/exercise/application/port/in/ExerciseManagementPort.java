package com.myfitness.exercise.application.port.in;

import com.myfitness.exercise.application.port.in.catalog.ExerciseView.Category;
import com.myfitness.exercise.application.port.in.catalog.ExerciseView;

public interface ExerciseManagementPort {
    ExerciseView createCustom(
            Long userId,
            String name,
            Category category);

    ExerciseView updateCustom(
            Long userId,
            Long exerciseId,
            String name,
            Category category);

    void deleteCustom(Long userId, Long exerciseId);
}
