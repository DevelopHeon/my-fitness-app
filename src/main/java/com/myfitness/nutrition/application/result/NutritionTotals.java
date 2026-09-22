package com.myfitness.nutrition.application.result;

import java.math.BigDecimal;

public record NutritionTotals(
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams
) {
    public static NutritionTotals zero() {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        return new NutritionTotals(zero, zero, zero, zero);
    }

    public NutritionTotals add(NutritionTotals other) {
        return new NutritionTotals(
                calories.add(other.calories),
                carbohydrateGrams.add(other.carbohydrateGrams),
                proteinGrams.add(other.proteinGrams),
                fatGrams.add(other.fatGrams));
    }

    public NutritionTotals subtract(NutritionTotals other) {
        return new NutritionTotals(
                calories.subtract(other.calories),
                carbohydrateGrams.subtract(other.carbohydrateGrams),
                proteinGrams.subtract(other.proteinGrams),
                fatGrams.subtract(other.fatGrams));
    }
}
