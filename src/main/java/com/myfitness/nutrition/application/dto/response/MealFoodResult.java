package com.myfitness.nutrition.application.dto.response;

import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MealFoodResult(
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
    public static MealFoodResult from(MealFood item) {
        return new MealFoodResult(item.getId(), item.getMeal().getMealDate(), item.getMeal().getMealType(),
                item.getFoodName(), item.getCalories(), item.getCarbohydrateGrams(), item.getProteinGrams(),
                item.getFatGrams(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
