package com.myfitness.nutrition.application.result;

import java.time.LocalDate;
import java.util.List;

public record DailyNutritionResult(
        LocalDate date,
        NutritionGoalResult goal,
        NutritionTotals consumed,
        NutritionTotals remaining,
        List<MealSectionResult> meals
) {}
