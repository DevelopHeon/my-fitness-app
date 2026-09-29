package com.myfitness.routine.application.dto.request;

import com.myfitness.exercise.domain.model.ExerciseType;

public record ExerciseSelection(
        ExerciseType exerciseType,
        Long exerciseId
) {}
