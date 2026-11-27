package com.myfitness.nutrition.application.usecase.response;

import com.myfitness.nutrition.domain.model.MealType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NutritionCalendarDayResult(
        LocalDate date,
        BigDecimal calories,
        List<String> mealTypes
) {
    public static NutritionCalendarDayResult from(LocalDate date, List<MealFoodResult> items) {
        BigDecimal calories = items.stream()
                .map(MealFoodResult::calories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> types = items.stream()
                .map(item -> MealType.valueOf(item.mealType()))
                .distinct()
                .sorted()
                .map(MealType::name)
                .toList();
        return new NutritionCalendarDayResult(date, calories, types);
    }
}
