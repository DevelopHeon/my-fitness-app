package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.port.in.NutritionCalendarDayView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NutritionCalendarDayResponse(
        LocalDate date,
        BigDecimal calories,
        List<String> mealTypes
) {
    public static NutritionCalendarDayResponse from(NutritionCalendarDayView result) {
        return new NutritionCalendarDayResponse(result.date(), result.calories(), result.mealTypes());
    }
}
