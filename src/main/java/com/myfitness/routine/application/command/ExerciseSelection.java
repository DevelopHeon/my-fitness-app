package com.myfitness.routine.application.command;

import com.myfitness.exercise.domain.model.ExerciseType;

public record ExerciseSelection(
        ExerciseType exerciseType,
        Long exerciseId
) {}
