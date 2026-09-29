package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.dto.response.NutritionTotals;
import java.math.BigDecimal;

public record NutritionTotalsResponse(
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams
) {
    public static NutritionTotalsResponse from(
            NutritionTotals totals) {
        if (totals == null) {
            return null;
        }
        return new NutritionTotalsResponse(
                totals.calories(),
                totals.carbohydrateGrams(),
                totals.proteinGrams(),
                totals.fatGrams());
    }
}
