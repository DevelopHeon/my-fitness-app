package com.myfitness.nutrition.application.dto.response;

import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.NutritionGoal;
import java.math.BigDecimal;

public record NutritionTotals(
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams
) {
    public static NutritionTotals from(MealFood item) {
        return new NutritionTotals(
                item.getCalories(),
                item.getCarbohydrateGrams(),
                item.getProteinGrams(),
                item.getFatGrams());
    }

    public static NutritionTotals from(NutritionGoal goal) {
        return new NutritionTotals(
                goal.getCalories(),
                goal.getCarbohydrateGrams(),
                goal.getProteinGrams(),
                goal.getFatGrams());
    }

    public static NutritionTotals zero() {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        return new NutritionTotals(zero, zero, zero, zero);
    }

    public NutritionTotals add(NutritionTotals other) {
        return new NutritionTotals(
                add(calories, other.calories),
                add(carbohydrateGrams, other.carbohydrateGrams),
                add(proteinGrams, other.proteinGrams),
                add(fatGrams, other.fatGrams));
    }

    public NutritionTotals subtract(NutritionTotals other) {
        return new NutritionTotals(
                subtract(calories, other.calories),
                subtract(carbohydrateGrams, other.carbohydrateGrams),
                subtract(proteinGrams, other.proteinGrams),
                subtract(fatGrams, other.fatGrams));
    }
    private static BigDecimal add(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? null : left.add(right);
    }

    private static BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? null : left.subtract(right);
    }
}
