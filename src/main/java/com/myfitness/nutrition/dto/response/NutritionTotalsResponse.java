package com.myfitness.nutrition.dto.response;

import java.math.BigDecimal;

public record NutritionTotalsResponse(
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams
) {
    public static NutritionTotalsResponse zero() {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        return new NutritionTotalsResponse(zero, zero, zero, zero);
    }

    public NutritionTotalsResponse add(NutritionTotalsResponse other) {
        return new NutritionTotalsResponse(
                calories.add(other.calories),
                carbohydrateGrams.add(other.carbohydrateGrams),
                proteinGrams.add(other.proteinGrams),
                fatGrams.add(other.fatGrams));
    }

    public NutritionTotalsResponse subtract(NutritionTotalsResponse other) {
        return new NutritionTotalsResponse(
                calories.subtract(other.calories),
                carbohydrateGrams.subtract(other.carbohydrateGrams),
                proteinGrams.subtract(other.proteinGrams),
                fatGrams.subtract(other.fatGrams));
    }
}
