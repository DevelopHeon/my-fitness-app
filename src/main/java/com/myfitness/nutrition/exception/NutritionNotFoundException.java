package com.myfitness.nutrition.exception;

public class NutritionNotFoundException extends RuntimeException {
    public NutritionNotFoundException(String message) {
        super(message);
    }
}
