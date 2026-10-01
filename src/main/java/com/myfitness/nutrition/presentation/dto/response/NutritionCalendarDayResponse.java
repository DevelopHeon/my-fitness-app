package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.dto.response.NutritionCalendarDayResult;
import com.myfitness.nutrition.domain.model.MealType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NutritionCalendarDayResponse(
        LocalDate date,
        BigDecimal calories,
        List<MealType> mealTypes
) {
    public static NutritionCalendarDayResponse from(NutritionCalendarDayResult result) {
        return new NutritionCalendarDayResponse(result.date(), result.calories(), result.mealTypes());
    }
}
