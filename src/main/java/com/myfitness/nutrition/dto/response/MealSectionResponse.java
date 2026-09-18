package com.myfitness.nutrition.dto.response;

import com.myfitness.nutrition.domain.MealType;
import java.util.List;

public record MealSectionResponse(
        MealType mealType,
        List<MealFoodResponse> items,
        NutritionTotalsResponse total
) {}
