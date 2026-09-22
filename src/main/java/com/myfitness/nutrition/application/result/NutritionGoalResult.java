package com.myfitness.nutrition.application.result;

import com.myfitness.nutrition.domain.model.NutritionGoal;
import java.math.BigDecimal;
import java.time.Instant;

public record NutritionGoalResult(
        Long id,
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams,
        Instant updatedAt
) {
    public static NutritionGoalResult from(NutritionGoal goal) {
        if (goal == null) {
            return null;
        }
        return new NutritionGoalResult(
                goal.getId(),
                goal.getCalories(),
                goal.getCarbohydrateGrams(),
                goal.getProteinGrams(),
                goal.getFatGrams(),
                goal.getUpdatedAt());
    }
}
