package com.myfitness.exercise.application.port.in;

import com.myfitness.exercise.domain.model.ExerciseCategory;
import com.myfitness.exercise.domain.model.ExerciseReference;

public interface ExerciseManagementUseCase {
    ExerciseReference createCustom(
            Long userId,
            String name,
            ExerciseCategory category);
}
