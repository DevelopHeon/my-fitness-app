package com.myfitness.nutrition.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DailyNutritionResponse(
        LocalDate date,
        NutritionGoalResponse goal,
        NutritionTotalsResponse consumed,
        NutritionTotalsResponse remaining,
        List<MealSectionResponse> meals
) {}
