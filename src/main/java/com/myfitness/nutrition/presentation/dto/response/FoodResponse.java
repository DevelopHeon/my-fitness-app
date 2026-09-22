package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.domain.model.Food;
import com.myfitness.nutrition.domain.model.ServingUnit;
import java.math.BigDecimal;
import java.time.Instant;

public record FoodResponse(
        Long id,
        String name,
        BigDecimal servingAmount,
        ServingUnit servingUnit,
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams,
        Instant createdAt,
        Instant updatedAt
) {
    public static FoodResponse from(Food food) {
        return new FoodResponse(
                food.getId(),
                food.getName(),
                food.getServingAmount(),
                food.getServingUnit(),
                food.getCalories(),
                food.getCarbohydrateGrams(),
                food.getProteinGrams(),
                food.getFatGrams(),
                food.getCreatedAt(),
                food.getUpdatedAt());
    }
}
