package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.result.DailyNutritionResult;
import java.time.LocalDate;
import java.util.List;

public record DailyNutritionResponse(
        LocalDate date,
        NutritionGoalResponse goal,
        NutritionTotalsResponse consumed,
        NutritionTotalsResponse remaining,
        List<MealSectionResponse> meals
) {
    public static DailyNutritionResponse from(
            DailyNutritionResult result) {
        return new DailyNutritionResponse(
                result.date(),
                result.goal() == null
                        ? null
                        : NutritionGoalResponse.from(result.goal()),
                NutritionTotalsResponse.from(result.consumed()),
                NutritionTotalsResponse.from(result.remaining()),
                result.meals().stream()
                        .map(MealSectionResponse::from)
                        .toList());
    }
}
