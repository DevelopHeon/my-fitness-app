package com.myfitness.nutrition.application.result;

import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import java.util.List;

public record MealSectionResult(
        MealType mealType,
        List<MealFood> items,
        NutritionTotals total
) {}
