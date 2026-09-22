package com.myfitness.nutrition.domain.model;

public enum ServingUnit {
    G("g"),
    ML("ml"),
    COUNT("개");

    private final String label;

    ServingUnit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
