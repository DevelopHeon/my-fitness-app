package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.dto.response.MealSectionResult;
import com.myfitness.nutrition.domain.model.MealType;
import java.util.List;

public record MealSectionResponse(
        MealType mealType,
        List<MealFoodResponse> items,
        NutritionTotalsResponse total
) {
    public static MealSectionResponse from(
            MealSectionResult result) {
        return new MealSectionResponse(
                result.mealType(),
                result.items().stream()
                        .map(MealFoodResponse::from)
                        .toList(),
                NutritionTotalsResponse.from(result.total()));
    }
}
