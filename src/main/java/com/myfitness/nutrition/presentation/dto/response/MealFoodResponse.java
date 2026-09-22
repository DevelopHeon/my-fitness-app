package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.result.MealFoodResult;
import com.myfitness.nutrition.domain.model.ServingUnit;
import java.math.BigDecimal;

public record MealFoodResponse(
        Long id,
        Long sourceFoodId,
        String foodName,
        BigDecimal servingAmount,
        ServingUnit servingUnit,
        BigDecimal caloriesPerServing,
        BigDecimal carbohydrateGramsPerServing,
        BigDecimal proteinGramsPerServing,
        BigDecimal fatGramsPerServing,
        BigDecimal servings,
        NutritionTotalsResponse total
) {
    public static MealFoodResponse from(MealFoodResult item) {
        return new MealFoodResponse(
                item.id(),
                item.sourceFoodId(),
                item.foodName(),
                item.servingAmount(),
                item.servingUnit(),
                item.caloriesPerServing(),
                item.carbohydrateGramsPerServing(),
                item.proteinGramsPerServing(),
                item.fatGramsPerServing(),
                item.servings(),
                NutritionTotalsResponse.from(item.total()));
    }
}
