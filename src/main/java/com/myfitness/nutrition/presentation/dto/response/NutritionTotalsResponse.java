package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.port.in.NutritionTotalsView;
import java.math.BigDecimal;

public record NutritionTotalsResponse(
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams
) {
    public static NutritionTotalsResponse from(
            NutritionTotalsView totals) {
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
