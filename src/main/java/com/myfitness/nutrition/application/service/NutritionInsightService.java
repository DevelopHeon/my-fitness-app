package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.application.dto.response.DailyNutritionResult;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery;
import com.myfitness.nutrition.domain.model.MealFood;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NutritionInsightService implements NutritionInsightQuery {
    private static final int AI_USAGE_HISTORY_LIMIT = 50;
    private final NutritionApplicationService nutritionApplicationService;
    private final MealService mealService;

    public NutritionInsightService(
            NutritionApplicationService nutritionApplicationService, MealService mealService) {
        this.nutritionApplicationService = nutritionApplicationService;
        this.mealService = mealService;
    }

    @Override
    public NutritionDayInsight getDay(Long userId, LocalDate date) {
        DailyNutritionResult daily =
                nutritionApplicationService.daily(userId, date);
        List<String> history = mealService.listRecentUsageHistory(userId, AI_USAGE_HISTORY_LIMIT).stream()
                .map(MealFood::getFoodName).toList();
        Map<String, Long> counts = history.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<String> recent = history.stream().distinct().limit(5).toList();
        List<String> frequent = history.stream().distinct()
                .sorted(Comparator.<String>comparingLong(counts::get).reversed()).limit(5).toList();

        return NutritionDayInsight.from(daily, recent, frequent);
    }
}
