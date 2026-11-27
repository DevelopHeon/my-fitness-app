package com.myfitness.exercise.application.port.in.catalog;

public record ExerciseView(Long id, Type type, String name, Category category, Long ownerUserId) {
    public enum Type {
        DEFAULT, CUSTOM
    }

    public enum Category {
        CHEST, SHOULDER, BACK, ARM, ABS, LEGS
    }
}
