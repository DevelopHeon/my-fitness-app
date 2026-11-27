package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.port.in.MealFoodView;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MealFoodResponse(
        Long id,
        LocalDate mealDate,
        String mealType,
        String foodName,
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams,
        Instant createdAt,
        Instant updatedAt
) {
    public static MealFoodResponse from(MealFoodView item) {
        return new MealFoodResponse(item.id(), item.mealDate(), item.mealType(), item.foodName(),
                item.calories(), item.carbohydrateGrams(), item.proteinGrams(), item.fatGrams(),
                item.createdAt(), item.updatedAt());
    }
}
