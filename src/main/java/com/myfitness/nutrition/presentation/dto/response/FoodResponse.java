package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.result.FoodResult;
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
    public static FoodResponse from(FoodResult food) {
        return new FoodResponse(
                food.id(),
                food.name(),
                food.servingAmount(),
                food.servingUnit(),
                food.calories(),
                food.carbohydrateGrams(),
                food.proteinGrams(),
                food.fatGrams(),
                food.createdAt(),
                food.updatedAt());
    }
}
