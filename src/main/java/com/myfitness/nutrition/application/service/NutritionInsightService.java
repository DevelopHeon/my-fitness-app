package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery;
import com.myfitness.nutrition.application.result.DailyNutritionResult;
import com.myfitness.nutrition.application.result.FoodSuggestionsResult;
import com.myfitness.nutrition.application.result.NutritionTotals;
import com.myfitness.nutrition.domain.model.NutritionGoal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NutritionInsightService implements NutritionInsightQuery {
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
                nutritionApplicationService.suggestions(userId);

        return new NutritionDayInsight(
                daily.date(),
                macro(daily.goal()),
                macro(daily.consumed()),
                macro(daily.remaining()),
                suggestions.recent().stream()
                        .map(food -> food.getName())
                        .toList(),
                suggestions.frequent().stream()
                        .map(food -> food.getName())
                        .toList());
    }

    private static MacroInsight macro(NutritionGoal goal) {
        if (goal == null) {
            return null;
        }
        return new MacroInsight(
                goal.getCalories(),
                goal.getCarbohydrateGrams(),
                goal.getProteinGrams(),
                goal.getFatGrams());
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
