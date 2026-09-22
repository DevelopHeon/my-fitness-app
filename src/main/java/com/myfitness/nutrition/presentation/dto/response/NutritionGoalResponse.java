package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.result.NutritionGoalResult;
import java.math.BigDecimal;
import java.time.Instant;

public record NutritionGoalResponse(
        Long id,
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams,
        Instant updatedAt
) {
    public static NutritionGoalResponse from(NutritionGoalResult goal) {
        if (goal == null) {
            return null;
        }
        return new NutritionGoalResponse(
                goal.id(),
                goal.calories(),
                goal.carbohydrateGrams(),
                goal.proteinGrams(),
                goal.fatGrams(),
                goal.updatedAt());
    }
}
