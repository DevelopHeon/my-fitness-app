package com.myfitness.exercise.application.usecase;

public record ExerciseValue(Long id, String type, String name, String category, Long ownerUserId) {
}
