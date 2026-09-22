package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.domain.model.MealFood;
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
    public static MealFoodResponse from(MealFood item) {
        return new MealFoodResponse(
                item.getId(),
                item.getSourceFoodId(),
                item.getFoodName(),
                item.getServingAmount(),
                item.getServingUnit(),
                item.getCaloriesPerServing(),
                item.getCarbohydrateGramsPerServing(),
                item.getProteinGramsPerServing(),
                item.getFatGramsPerServing(),
                item.getServings(),
                new NutritionTotalsResponse(
                        item.totalCalories(),
                        item.totalCarbohydrateGrams(),
                        item.totalProteinGrams(),
                        item.totalFatGrams()));
    }
}
