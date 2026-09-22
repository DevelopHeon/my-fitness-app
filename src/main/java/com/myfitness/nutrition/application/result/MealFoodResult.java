package com.myfitness.nutrition.application.result;

import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.ServingUnit;
import java.math.BigDecimal;

public record MealFoodResult(
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
        NutritionTotals total
) {
    public static MealFoodResult from(MealFood item) {
        return new MealFoodResult(
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
                new NutritionTotals(
                        item.totalCalories(),
                        item.totalCarbohydrateGrams(),
                        item.totalProteinGrams(),
                        item.totalFatGrams()));
    }
}
