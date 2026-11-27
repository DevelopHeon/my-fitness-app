package com.myfitness.nutrition.application.port.in;

import java.util.List;

public record MealSectionView(
        String mealType,
        List<MealFoodView> items,
        NutritionTotalsView total
) {}
