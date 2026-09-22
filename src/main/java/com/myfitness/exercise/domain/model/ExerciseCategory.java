package com.myfitness.exercise.domain.model;

public enum ExerciseCategory {
    CHEST("가슴"),
    SHOULDER("어깨"),
    BACK("등"),
    ARM("팔"),
    ABS("복근"),
    LEGS("하체");

    private final String label;

    ExerciseCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
