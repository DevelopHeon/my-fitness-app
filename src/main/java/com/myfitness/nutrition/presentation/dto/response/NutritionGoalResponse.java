package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.domain.model.NutritionGoal;
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
    public static NutritionGoalResponse from(NutritionGoal goal) {
        if (goal == null) {
            return null;
        }
        return new NutritionGoalResponse(
                goal.getId(),
                goal.getCalories(),
                goal.getCarbohydrateGrams(),
                goal.getProteinGrams(),
                goal.getFatGrams(),
                goal.getUpdatedAt());
    }

}