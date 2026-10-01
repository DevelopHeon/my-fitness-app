package com.myfitness.nutrition.application.port.in.insight;

import com.myfitness.nutrition.application.dto.response.DailyNutritionResult;
import com.myfitness.nutrition.application.dto.response.NutritionGoalResult;
import com.myfitness.nutrition.application.dto.response.NutritionTotals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface NutritionInsightQuery {
    NutritionDayInsight getDay(Long userId, LocalDate date);

    record NutritionDayInsight(
            LocalDate date,
            MacroInsight goal,
            MacroInsight consumed,
            MacroInsight remaining,
            List<String> recentFoods,
            List<String> frequentFoods
    ) {
        public static NutritionDayInsight from(
                DailyNutritionResult daily, List<String> recentFoods, List<String> frequentFoods) {
            return new NutritionDayInsight(
                    daily.date(),
                    MacroInsight.from(daily.goal()),
                    MacroInsight.from(daily.consumed()),
                    MacroInsight.from(daily.remaining()),
                    recentFoods,
                    frequentFoods);
        }
    }

    record MacroInsight(
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams
    ) {
        public static MacroInsight from(NutritionGoalResult goal) {
            if (goal == null) {
                return null;
            }
            return new MacroInsight(
                    goal.calories(),
                    goal.carbohydrateGrams(),
                    goal.proteinGrams(),
                    goal.fatGrams());
        }

        public static MacroInsight from(NutritionTotals totals) {
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
}
