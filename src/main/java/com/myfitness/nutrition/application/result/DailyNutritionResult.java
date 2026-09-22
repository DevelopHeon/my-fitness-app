package com.myfitness.nutrition.application.result;

import com.myfitness.nutrition.domain.model.NutritionGoal;
import java.time.LocalDate;
import java.util.List;

public record DailyNutritionResult(
        LocalDate date,
        NutritionGoal goal,
        NutritionTotals consumed,
        NutritionTotals remaining,
        List<MealSectionResult> meals
) {}
