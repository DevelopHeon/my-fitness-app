package com.myfitness.nutrition.application.usecase.response;

import java.util.List;

public record MealSectionResult(
        String mealType,
        List<MealFoodResult> items,
        NutritionTotals total
) {}
