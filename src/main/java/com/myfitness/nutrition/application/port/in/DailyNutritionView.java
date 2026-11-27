package com.myfitness.nutrition.application.port.in;

import java.time.LocalDate;
import java.util.List;

public record DailyNutritionView(
        LocalDate date,
        NutritionGoalView goal,
        NutritionTotalsView consumed,
        NutritionTotalsView remaining,
        List<MealSectionView> meals
) {}
