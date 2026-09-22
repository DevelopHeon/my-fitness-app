package com.myfitness.nutrition.application.exception;

public class NutritionNotFoundException extends RuntimeException {
    public NutritionNotFoundException(String message) {
        super(message);
    }
}
