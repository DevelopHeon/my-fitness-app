package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery;
import com.myfitness.nutrition.application.result.DailyNutritionResult;
import com.myfitness.nutrition.application.result.FoodSuggestionsResult;
import com.myfitness.nutrition.application.result.NutritionTotals;
import com.myfitness.nutrition.application.result.NutritionGoalResult;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NutritionInsightService implements NutritionInsightQuery {
    private static final int AI_USAGE_HISTORY_LIMIT = 50;
    private final NutritionApplicationService nutritionApplicationService;

    public NutritionInsightService(
            NutritionApplicationService nutritionApplicationService) {
        this.nutritionApplicationService = nutritionApplicationService;
    }

    @Override
    public NutritionDayInsight getDay(Long userId, LocalDate date) {
        DailyNutritionResult daily =
                nutritionApplicationService.daily(userId, date);
        FoodSuggestionsResult suggestions =
                nutritionApplicationService.suggestions(
                        userId,
                        AI_USAGE_HISTORY_LIMIT);

        return new NutritionDayInsight(
                daily.date(),
                macro(daily.goal()),
                macro(daily.consumed()),
                macro(daily.remaining()),
                suggestions.recent().stream()
                        .map(food -> food.name())
                        .toList(),
                suggestions.frequent().stream()
                        .map(food -> food.name())
                        .toList());
    }

    private static MacroInsight macro(NutritionGoalResult goal) {
        if (goal == null) {
            return null;
        }
        return new MacroInsight(
                goal.calories(),
                goal.carbohydrateGrams(),
                goal.proteinGrams(),
                goal.fatGrams());
    }

    private static MacroInsight macro(NutritionTotals totals) {
        if (totals == null) {
            return null;
        }
        return new MacroInsight(
                totals.calories(),
                totals.carbohydrateGrams(),
                totals.proteinGrams(),
                totals.fatGrams());
    }
}
