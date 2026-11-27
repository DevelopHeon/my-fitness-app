package com.myfitness.exercise.application.usecase;

public interface ExerciseManagementUseCase {
    ExerciseValue createCustom(
            Long userId,
            String name,
            String category);

    ExerciseValue updateCustom(
            Long userId,
            Long exerciseId,
            String name,
            String category);

    void deleteCustom(Long userId, Long exerciseId);
}
