package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.dto.response.MealFoodResult;
import com.myfitness.nutrition.domain.model.MealType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MealFoodResponse(
        Long id,
        LocalDate mealDate,
        MealType mealType,
        String foodName,
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams,
        Instant createdAt,
        Instant updatedAt
) {
    public static MealFoodResponse from(MealFoodResult item) {
        return new MealFoodResponse(item.id(), item.mealDate(), item.mealType(), item.foodName(),
                item.calories(), item.carbohydrateGrams(), item.proteinGrams(), item.fatGrams(),
                item.createdAt(), item.updatedAt());
    }
}
