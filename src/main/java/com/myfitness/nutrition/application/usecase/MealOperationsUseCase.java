package com.myfitness.nutrition.application.usecase;

import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface MealOperationsUseCase {
    MealFood addFood(Long userId, LocalDate mealDate, MealType mealType, String foodName,
            BigDecimal calories, BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat);

    MealFood getOwnedItem(Long userId, Long itemId);

    MealFood updateItem(MealFood item, LocalDate date, MealType type, String name,
            BigDecimal calories, BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat);

    void deleteItem(MealFood item);
}
