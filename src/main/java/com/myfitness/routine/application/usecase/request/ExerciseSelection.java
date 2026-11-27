package com.myfitness.routine.application.usecase.request;

public record ExerciseSelection(
        String exerciseType,
        Long exerciseId
) {}
