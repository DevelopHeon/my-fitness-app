package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.application.usecase.response.DailyNutritionResult;
import com.myfitness.nutrition.application.usecase.response.NutritionGoalResult;
import com.myfitness.nutrition.application.usecase.response.NutritionTotals;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery;
import com.myfitness.nutrition.application.usecase.NutritionInsightUseCase;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class NutritionInsightPortService implements NutritionInsightQuery {
    private final NutritionInsightUseCase useCase;

    public NutritionInsightPortService(NutritionInsightUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public NutritionDayInsight getDay(Long userId, LocalDate date) {
        NutritionInsightUseCase.Day result = useCase.getDay(userId, date);
        DailyNutritionResult daily = result.daily();
        NutritionGoalResult goal = daily.goal();
        MacroInsight goalInsight = goal == null ? null : new MacroInsight(
                goal.calories(), goal.carbohydrateGrams(), goal.proteinGrams(), goal.fatGrams());
        return new NutritionDayInsight(
                daily.date(), goalInsight, toMacro(daily.consumed()), toMacro(daily.remaining()),
                result.recentFoods(), result.frequentFoods());
    }

    private static MacroInsight toMacro(NutritionTotals totals) {
        return totals == null ? null : new MacroInsight(
                totals.calories(), totals.carbohydrateGrams(), totals.proteinGrams(), totals.fatGrams());
    }
}
