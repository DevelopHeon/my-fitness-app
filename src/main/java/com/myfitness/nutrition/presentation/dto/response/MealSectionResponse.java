package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.port.in.MealSectionView;
import java.util.List;

public record MealSectionResponse(
        String mealType,
        List<MealFoodResponse> items,
        NutritionTotalsResponse total
) {
    public static MealSectionResponse from(
            MealSectionView result) {
        return new MealSectionResponse(
                result.mealType(),
                result.items().stream()
                        .map(MealFoodResponse::from)
                        .toList(),
                NutritionTotalsResponse.from(result.total()));
    }
}
