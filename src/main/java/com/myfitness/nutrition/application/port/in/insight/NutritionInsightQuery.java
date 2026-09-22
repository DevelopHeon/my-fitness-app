package com.myfitness.nutrition.application.port.in.insight;

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
    ) {}

    record MacroInsight(
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams
    ) {}
}
