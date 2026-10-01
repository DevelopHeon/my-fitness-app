package com.myfitness.nutrition.application.dto.response;

import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NutritionCalendarDayResult(
        LocalDate date,
        BigDecimal calories,
        List<MealType> mealTypes
) {
    public static NutritionCalendarDayResult from(LocalDate date, List<MealFood> items) {
        BigDecimal calories = items.stream()
                .map(MealFood::getCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<MealType> types = items.stream()
                .map(item -> item.getMeal().getMealType())
                .distinct()
                .sorted()
                .toList();
        return new NutritionCalendarDayResult(date, calories, types);
    }
}
